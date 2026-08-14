package com.example.spendolive.publicdata.price.service;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.example.spendolive.publicdata.price.domain.ConsumerPriceComparisonDTO;
import com.example.spendolive.publicdata.price.domain.ConsumerProductDTO;
import com.example.spendolive.publicdata.price.domain.ConsumerStoreDTO;

@Service
public class ConsumerPriceServiceImpl implements ConsumerPriceService {

    // 가이드의 네 가지 오퍼레이션 중 상품·판매점·가격 조회를 사용
    private static final String PRODUCT_OPERATION = "getProductInfoSvc.do";
    private static final String STORE_OPERATION = "getStoreInfoSvc.do";
    private static final String PRICE_OPERATION = "getProductPriceInfoSvc";
    private static final DateTimeFormatter API_DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;
    private static final DateTimeFormatter VIEW_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
    private static final Duration MASTER_CACHE_DURATION = Duration.ofHours(6);
    private static final int MAX_PRODUCT_RESULTS = 20;
    private static final int MAX_INSPECT_WEEK_LOOKBACK = 8;

    private final HttpClient httpClient;
    private final String serviceKey;
    private final String baseUrl;
    private final Object productCacheLock = new Object();
    private final Object storeCacheLock = new Object();
    private final AtomicBoolean productRefreshRunning = new AtomicBoolean(false);
    private final AtomicBoolean storeRefreshRunning = new AtomicBoolean(false);

    private volatile Instant productCacheTime = Instant.EPOCH;
    private volatile Instant storeCacheTime = Instant.EPOCH;
    private volatile List<ConsumerProductDTO> productCache = List.of();
    private volatile Map<String, ConsumerStoreDTO> storeCache = Map.of();

    public ConsumerPriceServiceImpl(
            @Value("${consumer-price.api.service-key:${CONSUMER_PRICE_API_KEY:}}") String serviceKey,
            @Value("${consumer-price.api.base-url:https://apis.data.go.kr/B551919/ProductPriceInfoService}") String baseUrl) {
        this.serviceKey = serviceKey == null ? "" : serviceKey.trim();
        this.baseUrl = isBlank(baseUrl)
                ? "https://apis.data.go.kr/B551919/ProductPriceInfoService"
                : removeTrailingSlash(baseUrl);
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(7)).build();
    }

    // 서버 기동 후 상품·판매점 기준정보를 미리 받아 첫 검색 대기 시간을 줄인다
    @EventListener(ApplicationReadyEvent.class)
    public void warmMasterCaches() {
        refreshProductsAsync();
        refreshStoresAsync();
    }

    @Override
    public List<ConsumerProductDTO> searchProducts(String keyword) throws Exception {
        String normalizedKeyword = normalizeSearchText(keyword);

        if (normalizedKeyword.isBlank()) {
            throw new IllegalArgumentException("검색할 상품명을 입력해주세요.");
        }

        // 전체 상품은 6시간 캐시하고 화면에는 일치 상품 최대 20개만 반환
        return loadProducts().stream()
                .filter(product -> normalizeSearchText(product.getGoodName()).contains(normalizedKeyword))
                .sorted(Comparator.comparing(ConsumerProductDTO::getGoodName, Comparator.nullsLast(String::compareTo)))
                .limit(MAX_PRODUCT_RESULTS)
                .collect(Collectors.toList());
    }

    @Override
    public ConsumerPriceComparisonDTO comparePrices(String goodId, String goodName) throws Exception {
        String normalizedGoodId = goodId == null ? "" : goodId.trim();

        if (normalizedGoodId.isBlank()) {
            throw new IllegalArgumentException("가격을 조회할 상품을 선택해주세요.");
        }

        // 가이드상 조사일은 금요일이므로 최근 금요일부터 최대 8주 전까지 데이터가 있는 날을 찾는다
        LocalDate latestFriday = LocalDate.now(KOREA_ZONE).with(TemporalAdjusters.previousOrSame(DayOfWeek.FRIDAY));
        PriceLookupResult lookupResult = null;

        for (int week = 0; week < MAX_INSPECT_WEEK_LOOKBACK; week++) {
            LocalDate inspectDate = latestFriday.minusWeeks(week);
            List<PriceRow> priceRows = loadPriceRows(normalizedGoodId, inspectDate);

            if (!priceRows.isEmpty()) {
                lookupResult = new PriceLookupResult(inspectDate, priceRows);
                break;
            }
        }

        if (lookupResult == null) {
            throw new IllegalStateException("최근 조사 가격을 찾지 못했습니다. 다른 상품을 선택해주세요.");
        }

        Map<String, ConsumerStoreDTO> stores = loadStores();
        Map<String, ConsumerPriceComparisonDTO.StorePrice> lowestStorePrices = new LinkedHashMap<>();

        for (PriceRow row : lookupResult.priceRows()) {
            if (row.price() <= 0 || row.entpId().isBlank()) {
                continue;
            }

            ConsumerStoreDTO store = stores.get(row.entpId());
            String storeName = store == null || isBlank(store.getEntpName()) ? "판매점 " + row.entpId() : store.getEntpName();
            String roadAddress = store == null ? "" : defaultString(store.getRoadAddrBasic());
            String xMapCoord = store == null ? "" : defaultString(store.getXMapCoord());
            String yMapCoord = store == null ? "" : defaultString(store.getYMapCoord());
            ConsumerPriceComparisonDTO.StorePrice storePrice = new ConsumerPriceComparisonDTO.StorePrice(
                    row.entpId(), storeName, roadAddress, xMapCoord, yMapCoord, row.price(), row.plusOneYn(), row.discountYn());

            // 같은 판매점 가격이 중복 응답되면 가장 낮은 가격 한 건만 사용
            lowestStorePrices.merge(row.entpId(), storePrice,
                    (before, after) -> before.getPrice() <= after.getPrice() ? before : after);
        }

        List<ConsumerPriceComparisonDTO.StorePrice> storePrices = lowestStorePrices.values().stream()
                .sorted(Comparator.comparingInt(ConsumerPriceComparisonDTO.StorePrice::getPrice)
                        .thenComparing(ConsumerPriceComparisonDTO.StorePrice::getStoreName))
                .collect(Collectors.toList());

        if (storePrices.isEmpty()) {
            throw new IllegalStateException("표시할 판매점 가격이 없습니다. 다른 상품을 선택해주세요.");
        }

        int lowestPrice = storePrices.get(0).getPrice();
        int highestPrice = storePrices.get(storePrices.size() - 1).getPrice();
        int averagePrice = (int) Math.round(storePrices.stream()
                .mapToInt(ConsumerPriceComparisonDTO.StorePrice::getPrice)
                .average()
                .orElse(0));
        String resolvedGoodName = isBlank(goodName) ? findProductName(normalizedGoodId) : goodName.trim();

        return new ConsumerPriceComparisonDTO(
                normalizedGoodId,
                resolvedGoodName,
                lookupResult.inspectDate().format(VIEW_DATE_FORMAT),
                lowestPrice,
                averagePrice,
                highestPrice,
                storePrices
        );
    }

    private List<ConsumerProductDTO> loadProducts() throws Exception {
        if (!productCache.isEmpty()) {
            // 만료된 캐시는 즉시 사용하고 새 기준정보는 백그라운드에서 갱신
            if (!isCacheValid(productCacheTime)) {
                refreshProductsAsync();
            }
            return productCache;
        }

        return refreshProducts();
    }

    private List<ConsumerProductDTO> refreshProducts() throws Exception {
        synchronized (productCacheLock) {
            if (isCacheValid(productCacheTime) && !productCache.isEmpty()) {
                return productCache;
            }

            Document document = requestXml(PRODUCT_OPERATION, Map.of());
            List<ConsumerProductDTO> products = new ArrayList<>();

            for (Element element : allElements(document)) {
                String productId = directChildText(element, "goodid");
                String productName = directChildText(element, "goodname");

                if (isBlank(productId) || isBlank(productName)) {
                    continue;
                }

                products.add(new ConsumerProductDTO(
                        productId,
                        productName,
                        directChildText(element, "goodtotalcnt"),
                        directChildText(element, "goodtotaldivcode")
                ));
            }

            productCache = List.copyOf(products);
            productCacheTime = Instant.now();
            return productCache;
        }
    }

    private void refreshProductsAsync() {
        if (!productRefreshRunning.compareAndSet(false, true)) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                refreshProducts();
            } catch (Exception ignored) {
                // 사전 캐시 실패 시 실제 검색 요청에서 다시 조회
            } finally {
                productRefreshRunning.set(false);
            }
        });
    }

    private Map<String, ConsumerStoreDTO> loadStores() throws Exception {
        if (!storeCache.isEmpty()) {
            // 만료된 캐시는 가격 비교에 그대로 사용하고 최신 판매점 정보만 비동기 갱신
            if (!isCacheValid(storeCacheTime)) {
                refreshStoresAsync();
            }
            return storeCache;
        }

        return refreshStores();
    }

    private Map<String, ConsumerStoreDTO> refreshStores() throws Exception {
        synchronized (storeCacheLock) {
            if (isCacheValid(storeCacheTime) && !storeCache.isEmpty()) {
                return storeCache;
            }

            Document document = requestXml(STORE_OPERATION, Map.of());
            Map<String, ConsumerStoreDTO> stores = new LinkedHashMap<>();

            for (Element element : allElements(document)) {
                String entpId = directChildText(element, "entpid");
                String entpName = directChildText(element, "entpname");

                if (isBlank(entpId) || isBlank(entpName)) {
                    continue;
                }

                String roadAddress = directChildText(element, "roadaddrbasic");
                if (isBlank(roadAddress)) {
                    roadAddress = directChildText(element, "plmkaddrbasic");
                }

                stores.putIfAbsent(entpId, new ConsumerStoreDTO(
                        entpId,
                        entpName,
                        roadAddress,
                        directChildText(element, "xmapcoord"),
                        directChildText(element, "ymapcoord")
                ));
            }

            storeCache = Map.copyOf(stores);
            storeCacheTime = Instant.now();
            return storeCache;
        }
    }

    private void refreshStoresAsync() {
        if (!storeRefreshRunning.compareAndSet(false, true)) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                refreshStores();
            } catch (Exception ignored) {
                // 사전 캐시 실패 시 실제 가격 조회 요청에서 다시 조회
            } finally {
                storeRefreshRunning.set(false);
            }
        });
    }

    private List<PriceRow> loadPriceRows(String goodId, LocalDate inspectDate) throws Exception {
        Document document = requestXml(PRICE_OPERATION, Map.of(
                "goodInspectDay", inspectDate.format(API_DATE_FORMAT),
                "goodId", goodId
        ));
        List<PriceRow> rows = new ArrayList<>();

        for (Element element : allElements(document)) {
            String priceText = directChildText(element, "goodprice");
            String entpId = directChildText(element, "entpid");
            String responseGoodId = directChildText(element, "goodid");

            if (isBlank(priceText) || isBlank(entpId) || (!isBlank(responseGoodId) && !goodId.equals(responseGoodId))) {
                continue;
            }

            try {
                rows.add(new PriceRow(
                        entpId,
                        Integer.parseInt(priceText.replace(",", "").trim()),
                        defaultYn(directChildText(element, "plusoneyn")),
                        defaultYn(directChildText(element, "gooddcyn"))
                ));
            } catch (NumberFormatException ignored) {
                // 숫자가 아닌 가격 한 건은 전체 조회 실패 대신 제외
            }
        }

        return rows;
    }

    private Document requestXml(String operation, Map<String, String> parameters) throws Exception {
        validateServiceKey();
        StringBuilder url = new StringBuilder(baseUrl).append('/').append(operation)
                .append("?serviceKey=").append(encode(serviceKey));

        for (Map.Entry<String, String> parameter : parameters.entrySet()) {
            url.append('&').append(encode(parameter.getKey())).append('=').append(encode(parameter.getValue()));
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url.toString()))
                .timeout(Duration.ofSeconds(12))
                .header("Accept", "application/xml, text/xml, */*")
                .GET()
                .build();
        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

        if (response.statusCode() != 200) {
            throw new IllegalStateException("한국소비자원 가격정보 API 응답 오류가 발생했습니다. HTTP " + response.statusCode());
        }

        Document document = parseXml(response.body());
        String resultCode = firstDescendantText(document, "resultcode");
        String resultMessage = firstDescendantText(document, "resultmsg");

        if (!isBlank(resultCode) && !"00".equals(resultCode)) {
            throw new IllegalStateException("한국소비자원 API 오류: " + resultCode + " " + defaultString(resultMessage));
        }

        return document;
    }

    private Document parseXml(byte[] xmlBytes) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        // 외부 XML의 DTD·외부 엔티티를 차단해 XXE 취약점을 방지
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);

        try (ByteArrayInputStream input = new ByteArrayInputStream(xmlBytes)) {
            Document document = factory.newDocumentBuilder().parse(input);
            document.getDocumentElement().normalize();
            return document;
        }
    }

    private List<Element> allElements(Document document) {
        NodeList nodes = document.getElementsByTagName("*");
        List<Element> elements = new ArrayList<>(nodes.getLength());

        for (int index = 0; index < nodes.getLength(); index++) {
            Node node = nodes.item(index);
            if (node instanceof Element element) {
                elements.add(element);
            }
        }

        return elements;
    }

    private String directChildText(Element parent, String expectedTagName) {
        NodeList children = parent.getChildNodes();

        for (int index = 0; index < children.getLength(); index++) {
            Node child = children.item(index);

            if (child instanceof Element element
                    && normalizeTagName(element.getTagName()).equals(normalizeTagName(expectedTagName))) {
                return element.getTextContent() == null ? "" : element.getTextContent().trim();
            }
        }

        return "";
    }

    private String firstDescendantText(Document document, String expectedTagName) {
        String normalizedExpectedTag = normalizeTagName(expectedTagName);

        for (Element element : allElements(document)) {
            if (normalizeTagName(element.getTagName()).equals(normalizedExpectedTag)) {
                return element.getTextContent() == null ? "" : element.getTextContent().trim();
            }
        }

        return "";
    }

    private String findProductName(String goodId) throws Exception {
        return loadProducts().stream()
                .filter(product -> Objects.equals(goodId, product.getGoodId()))
                .map(ConsumerProductDTO::getGoodName)
                .findFirst()
                .orElse("선택 상품");
    }

    private boolean isCacheValid(Instant cachedAt) {
        return cachedAt.plus(MASTER_CACHE_DURATION).isAfter(Instant.now());
    }

    private void validateServiceKey() {
        if (serviceKey.isBlank()) {
            throw new IllegalStateException("한국소비자원 API 인증키가 설정되지 않았습니다.");
        }
    }

    private String normalizeSearchText(String value) {
        return defaultString(value).replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }

    private String normalizeTagName(String value) {
        int colonIndex = value == null ? -1 : value.indexOf(':');
        String localName = colonIndex >= 0 ? value.substring(colonIndex + 1) : defaultString(value);
        return localName.toLowerCase(Locale.ROOT);
    }

    private String encode(String value) {
        return URLEncoder.encode(defaultString(value), StandardCharsets.UTF_8);
    }

    private String removeTrailingSlash(String value) {
        String normalized = defaultString(value).trim();
        while (normalized.endsWith("/")) normalized = normalized.substring(0, normalized.length() - 1);
        return normalized;
    }

    private String defaultYn(String value) {
        return "Y".equalsIgnoreCase(value) ? "Y" : "N";
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record PriceRow(String entpId, int price, String plusOneYn, String discountYn) {}
    private record PriceLookupResult(LocalDate inspectDate, List<PriceRow> priceRows) {}
}
