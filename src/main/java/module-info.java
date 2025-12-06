module qilletni.toolchain {
    uses dev.qilletni.api.auth.ServiceProvider;
    
    requires org.slf4j;
    requires qilletni.api;
    requires qilletni.impl;
    requires qilletni.docgen;
    requires qilletni.pkgutil;
    requires org.yaml.snakeyaml;
    requires info.picocli;
    requires com.google.gson;
    requires org.apache.logging.log4j.core;
}
