package org.jboss.as.console.spi;

/**
 * @author Heiko Braun
 * @date 3/26/12
 * @deprecated No longer necessary. Only used by {@link org.jboss.as.console.spi.SPIProcessor}.
 */
@Deprecated
public class ExtensionDeclaration {
    private String type;

    public ExtensionDeclaration(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}