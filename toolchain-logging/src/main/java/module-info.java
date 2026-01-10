module qilletni.toolchain.logging {
    requires org.apache.logging.log4j;
    requires org.apache.logging.log4j.core;
    requires org.slf4j;
    requires info.picocli;
    requires qilletni.api;
    exports dev.qilletni.toolchain.logging;
}
