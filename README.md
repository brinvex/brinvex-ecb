## Brinvex ECB

_Brinvex ECB_ is a lightweight Java library that simplifies working with the European Central Bank's Data API: 
https://data.ecb.europa.eu/help/api/overview.

### Features
#### Fetching EUR FX rates
This feature allows you to get foreign exchange rates for EUR against a specified currency (e.g., USD, CZK, ...) over a specified date range.
````    
SequencedMap<LocalDate, Double> fxRates = 
    ecbFetchService.fetch(new FxRequest("USD", startDayIncl, endDayIncl));
````
https://data.ecb.europa.eu/data/datasets/EXR/EXR.D.USD.EUR.SP00.A
https://data.ecb.europa.eu/data/datasets/EXR/EXR.D.CZK.EUR.SP00.A

#### Fetching Key Interest Rates
This feature allows you to retrieve Deposit Facility Interest Rates over a specified date range.
````    
SequencedMap<LocalDate, Double> interestRates = 
    ecbFetchService.fetch(new DepositFacilityRateRequest(startDayIncl, endDayIncl));
````
https://data.ecb.europa.eu/data/datasets/FM/FM.D.U2.EUR.4F.KR.DFR.LEV
#### Fetching Inflation data
This feature allows you to retrieve Harmonised Index of Consumer Prices (HICP) data over a specified date range.
Is it also possible to calculate year-over-year and month-over-month inflation growth factors based on fetched HICP data.
````    
record InflationResponse(
    BigDecimal cpi,
    BigDecimal momGrowthFactor,
    BigDecimal yoyGrowthFactor
) {}
SequencedMap<LocalDate, InflationResponse> inflatonData = 
    ecbFetchService.fetch(new HICPInflationRequest(
        startDayIncl, 
        endDayIncl, 
        CALCULATE_MOM_GROWTH, 
        CALCULATE_YOY_GROWTH
    ));
````
https://data.ecb.europa.eu/data/datasets/ICP/ICP.M.U2.N.000000.4.INX
https://data.ecb.europa.eu/data/datasets/ICP/ICP.M.U2.N.000000.4.ANR

### Maven and JPMS Setup
To use _Brinvex ECB_ in your Maven project, declare the following dependency in your project's pom file. 
No transitive dependencies are required during compilation or at runtime.
````
<properties>
     <brinvex-ecb.version>1.0.0</brinvex-ecb.version>
</properties>

<repository>
    <id>github-pubrepo-brinvex</id>
    <name>Github Public Repository - Brinvex</name>
    <url>https://github.com/brinvex/brinvex-pubrepo/raw/main/</url>
    <snapshots>
        <enabled>false</enabled>
    </snapshots>
</repository>

<dependency>
    <groupId>com.brinvex</groupId>
    <artifactId>brinvex-ecb</artifactId>
    <version>${brinvex-ecb.version}</version>
</dependency>
````
The library supports _JPMS_ and exports the module named ````com.brinvex.ecb````.

### Requirements
Java 21 or above

### License
The _Brinvex ECB_ is released under version 2.0 of the Apache License.



