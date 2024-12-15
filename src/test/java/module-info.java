module test.com.brinvex.ecb {
    requires com.brinvex.ecb;
    requires org.junit.jupiter.api;
    requires org.junit.jupiter.engine;
    opens test.com.brinvex.ecb to org.junit.platform.commons;
}