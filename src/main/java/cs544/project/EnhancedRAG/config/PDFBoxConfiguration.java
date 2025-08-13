package cs544.project.EnhancedRAG.config;

import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Configuration to suppress PDFBox font warnings and optimize PDF processing
 */
@Configuration
public class PDFBoxConfiguration {

    @PostConstruct
    public void configPDFBox() {
        // Disable PDFBox font caching to avoid font-related errors on system fonts
        System.setProperty("pdfbox.fontcache", "false");
        
        // Set PDFBox to use less strict font loading
        System.setProperty("sun.java2d.cmm", "sun.java2d.cmm.kcms.KcmsServiceProvider");
        
        // Suppress specific font-related warnings
        java.util.logging.Logger.getLogger("org.apache.pdfbox").setLevel(java.util.logging.Level.SEVERE);
        java.util.logging.Logger.getLogger("org.apache.fontbox").setLevel(java.util.logging.Level.SEVERE);
        java.util.logging.Logger.getLogger("org.apache.pdfbox.pdmodel.font").setLevel(java.util.logging.Level.SEVERE);
    }
}
