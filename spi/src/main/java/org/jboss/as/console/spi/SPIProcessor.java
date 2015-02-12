package org.jboss.as.console.spi;

import static javax.lang.model.SourceVersion.RELEASE_7;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.NoGatekeeper;
import org.jboss.as.console.client.plugins.RequiredResourcesMetaData;
import org.jboss.as.console.client.plugins.BootstrapOperation;
import org.jboss.as.console.client.plugins.RuntimeExtensionMetaData;
import org.jboss.as.console.client.plugins.SearchIndexMetaData;
import org.jboss.as.console.client.plugins.SubsystemExtensionMetaData;

/**
 * @author Heiko Braun
 */


@SupportedSourceVersion(RELEASE_7)
public class SPIProcessor extends AbstractProcessor {

    private static final String EXTENSION_TEMPLATE = "Extension.tmpl";
    private static final String EXTENSION_FILENAME = "org.jboss.as.console.client.core.gin.Composite";
    private static final String BINDING_TEMPLATE = "ExtensionBinding.tmpl";
    private final static String BINDING_FILENAME = "org.jboss.as.console.client.core.gin.CompositeBinding";
    private static final String BEAN_FACTORY_TEMPLATE = "BeanFactory.tmpl";
    private static final String BEAN_FACTORY_FILENAME = "org.jboss.as.console.client.shared.BeanFactory";
    private static final String SUBSYSTEM_FILENAME = "org.jboss.as.console.client.plugins.SubsystemRegistryImpl";
    private static final String SUBSYSTEM_TEMPLATE = "SubsystemExtensions.tmpl";
    private static final String REQUIRED_RESOURCES_FILENAME = "org.jboss.as.console.client.plugins.RequiredResourcesRegistryImpl";
    private static final String REQUIRED_RESOURCES_TEMPLATE = "RequiredResourcesRegistry.tmpl";
    private static final String SEARCH_INDEX_FILENAME = "org.jboss.as.console.client.plugins.SearchIndexRegistryImpl";
    private static final String SEARCH_INDEX_TEMPLATE = "SearchIndexRegistry.tmpl";
    private static final String RUNTIME_FILENAME = "org.jboss.as.console.client.plugins.RuntimeLHSItemExtensionRegistryImpl";
    private static final String RUNTIME_TEMPLATE = "RuntimeExtensions.tmpl";
    private static final String VERSION_INFO_FILENAME = "org.jboss.as.console.client.VersionInfo";
    private static final String VERSION_INFO_TEMPLATE = "VersionInfo.tmpl";

    private Filer filer;
    private ProcessingEnvironment processingEnv;
    private List<String> discoveredExtensions;
    private List<ExtensionDeclaration> discoveredBindings;
    private List<String> discoveredBeanFactories;
    private List<String> categoryClasses;
    private List<SubsystemExtensionMetaData> subsystemDeclararions;
    private List<RequiredResourcesMetaData> requiredResourcesDeclarations;
    private List<SearchIndexMetaData> searchIndexDeclarations;
    private List<BootstrapOperation> bootstrapOperations;
    private List<RuntimeExtensionMetaData> runtimeExtensions;
    private Set<String> modules = new LinkedHashSet<>();
    private Set<String> nameTokens;
    private List<ModuleConfig> moduleConfigs;
    private Map<String, String> gwtConfigProps;

    @Override
    public void init(ProcessingEnvironment env) {
        this.processingEnv = env;
        this.filer = env.getFiler();
        this.discoveredExtensions = new ArrayList<>();
        this.discoveredBindings = new ArrayList<>();
        this.discoveredBeanFactories = new ArrayList<>();
        this.categoryClasses = new ArrayList<>();
        this.subsystemDeclararions = new ArrayList<>();
        this.requiredResourcesDeclarations = new ArrayList<>();
        this.searchIndexDeclarations = new ArrayList<>();
        this.bootstrapOperations = new ArrayList<>();
        this.runtimeExtensions = new ArrayList<>();
        this.nameTokens = new HashSet<>();

        moduleConfigs = new ArrayList<ModuleConfig>();
        moduleConfigs.add(new ModuleConfig(filer, "App_base.gwt.xml.tmpl", "App.gwt.xml"));
        moduleConfigs.add(new ModuleConfig(filer, "App_WF.gwt.xml.tmpl", "App_WF.gwt.xml"));
        moduleConfigs.add(new ModuleConfig(filer, "App_WF_full.gwt.xml.tmpl", "App_WF_full.gwt.xml"));
        moduleConfigs.add(new ModuleConfig(filer, "App_WF_dev.gwt.xml.tmpl", "App_WF_dev.gwt.xml"));
        moduleConfigs.add(new ModuleConfig(filer, "App_RH.gwt.xml.tmpl", "App_RH.gwt.xml"));
        moduleConfigs.add(new ModuleConfig(filer, "App_RH_dev.gwt.xml.tmpl", "App_RH_dev.gwt.xml"));

        env.getMessager();
        parseGwtProperties();
    }

    private void parseGwtProperties() {
        Map<String, String> options = processingEnv.getOptions();
        gwtConfigProps = new HashMap<String, String>();
        for (String key : options.keySet()) {
            if (key.startsWith("gwt.")) {
                gwtConfigProps.put(key.substring(4, key.length()), options.get(key));
            }
        }
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> types = new HashSet<>();
        types.add(GinExtension.class.getName());
        types.add(GinExtensionBinding.class.getName());
        types.add(BeanFactoryExtension.class.getName());
        types.add(SubsystemExtension.class.getName());
        types.add(RuntimeExtension.class.getName());
        return types;
    }

    @Override
    public boolean process(Set<? extends TypeElement> typeElements, RoundEnvironment roundEnv) {

        if (!roundEnv.processingOver()) {

            System.out.println("=================================");
            System.out.println("Begin Components discovery ...");
            System.out.println("=================================");

            Set<? extends Element> extensionElements = roundEnv.getElementsAnnotatedWith(GinExtension.class);
            for (Element element : extensionElements) {
                handleGinExtensionElement(element);
            }

            System.out.println("=================================");
            System.out.println("Begin Bindings discovery ...");
            System.out.println("=================================");

            Set<? extends Element> extensionBindingElements = roundEnv
                    .getElementsAnnotatedWith(GinExtensionBinding.class);

            for (Element element : extensionBindingElements) {
                handleGinExtensionBindingElement(element);
            }

            System.out.println("=================================");
            System.out.println("Begin BeanFactory discovery ...");
            System.out.println("=================================");

            Set<? extends Element> beanFactoryElements = roundEnv.getElementsAnnotatedWith(BeanFactoryExtension.class);
            for (Element element : beanFactoryElements) {
                handleBeanFactoryElement(element);
            }

            System.out.println("=================================");
            System.out.println("Begin Subsystem discovery ...");
            System.out.println("=================================");

            Set<? extends Element> subsystemElements = roundEnv.getElementsAnnotatedWith(SubsystemExtension.class);
            for (Element element : subsystemElements) {
                handleSubsystemElement(element);
            }

            System.out.println("=================================");
            System.out.println("Parse RequiredResources metadata ...");
            System.out.println("=================================");
            Set<? extends Element> accessElements = roundEnv.getElementsAnnotatedWith(NameToken.class);

            for (Element element : accessElements) {
                handleRequiredResourcesElement(element);
            }

            System.out.println("=================================");
            System.out.println("Parse SearchIndex metadata ...");
            System.out.println("=================================");
            Set<? extends Element> searchIndexElements = roundEnv.getElementsAnnotatedWith(NameToken.class);

            for (Element element : searchIndexElements) {
                handleSearchIndexElement(element);
            }

            System.out.println("=================================");
            System.out.println("Begin Runtime Extension discovery ...");
            System.out.println("=================================");

            Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(RuntimeExtension.class);
            for (Element element : elements) {
                handleRuntimeExtensions(element);
            }
        }

        if (roundEnv.processingOver()) {
            try {
                // generate the actual implementation
                writeFiles();
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("Failed to process SPI artifacts");
            }

            System.out.println("=================================");
            System.out.println("SPI component discovery completed.");
        }
        return true;
    }

    private void handleRequiredResourcesElement(Element element) {
        List<? extends AnnotationMirror> annotationMirrors = element.getAnnotationMirrors();
        for (AnnotationMirror mirror : annotationMirrors) {
            final String annotationType = mirror.getAnnotationType().toString();

            if (annotationType.equals(NameToken.class.getName())) {
                NameToken nameToken = element.getAnnotation(NameToken.class);
                RequiredResources requiredResources = element.getAnnotation(RequiredResources.class);
                AccessControl accessControl = element.getAnnotation(AccessControl.class);

                if (accessControl != null && requiredResources != null) {
                    System.out.println("WARNING: Detected both @" + RequiredResources.class.getSimpleName() +
                            " and @" + AccessControl.class.getSimpleName() + " on #" + nameToken
                            + "! Only @" + RequiredResources.class.getSimpleName() + " will be processed.");
                }
                else if (requiredResources != null) {
                    String[] resources = requiredResources.resources();
                    boolean recursive = requiredResources.recursive();
                    String[] operations = requiredResources.operations();
                    addRequiredResource(nameToken.value()[0], resources, recursive, operations);
                }
                else if (accessControl != null) {
                    String[] resources = accessControl.resources();
                    boolean recursive = accessControl.recursive();
                    String[] operations = accessControl.operations();
                    addRequiredResource(nameToken.value()[0], resources, recursive, operations);

                } else if (element.getAnnotation(NoGatekeeper.class) == null) {
                    Name simpleName = element.getEnclosingElement() != null ? element.getEnclosingElement()
                            .getSimpleName() : element.getSimpleName();
                    System.out.println(
                            simpleName + "(#" + nameToken.value()[0] + ")" + " is missing @AccessControl annotation!");
                }
            }
        }
    }

    private void addRequiredResource(String token, String[] resources, boolean recursive, String[] operations) {
        for (String resourceAddress : resources) {
            RequiredResourcesMetaData declared = new RequiredResourcesMetaData(token, resourceAddress);
            declared.setRecursive(recursive);
            requiredResourcesDeclarations.add(declared);
        }

        for (String opString : operations) {
            if (!opString.contains("#")) {
                throw new IllegalArgumentException("Invalid operation string:" + opString);
            }
            BootstrapOperation op = new BootstrapOperation(token, opString);
            bootstrapOperations.add(op);
        }
    }

    private void handleSearchIndexElement(final Element element) {
        List<? extends AnnotationMirror> annotationMirrors = element.getAnnotationMirrors();
        for (AnnotationMirror mirror : annotationMirrors) {
            final String annotationType = mirror.getAnnotationType().toString();

            if (annotationType.equals(NameToken.class.getName())) {
                NameToken nameToken = element.getAnnotation(NameToken.class);
                AccessControl accessControl = element.getAnnotation(AccessControl.class);
                SearchIndex searchIndex = element.getAnnotation(SearchIndex.class);
                OperationMode operationMode = element.getAnnotation(OperationMode.class);

                if (accessControl != null) {
                    boolean standalone = true;
                    boolean domain = true;
                    String[] keywords = null;
                    boolean include = true;
                    if (searchIndex != null) {
                        keywords = searchIndex.keywords();
                        include = !searchIndex.exclude();
                    }
                    if (operationMode != null) {
                        standalone = operationMode.value() == OperationMode.Mode.STANDALONE;
                        domain = operationMode.value() == OperationMode.Mode.DOMAIN;
                    }
                    if (include) {
                        // excluded presenters are not part of the metadata!
                        SearchIndexMetaData searchIndexMetaData = new SearchIndexMetaData(nameToken.value()[0], standalone,
                                domain, accessControl.resources(), keywords);
                        searchIndexDeclarations.add(searchIndexMetaData);
                    }
                }
            }
        }
    }

    private void handleGinExtensionBindingElement(Element element) {
        String typeName = element.asType().toString();
        System.out.println("Binding: " + typeName);
        discoveredBindings.add(new ExtensionDeclaration(typeName));
    }

    private void handleRuntimeExtensions(Element element) {
        List<? extends AnnotationMirror> annotationMirrors = element.getAnnotationMirrors();
        for (AnnotationMirror mirror : annotationMirrors) {
            final String annotationType = mirror.getAnnotationType().toString();
            if (annotationType.equals(RuntimeExtension.class.getName())) {
                NameToken nameToken = element.getAnnotation(NameToken.class);
                RuntimeExtension extension = element.getAnnotation(RuntimeExtension.class);
                if (nameToken != null) {
                    System.out.println("Runtime Extension: " + extension.name() + " -> " + nameToken.value()[0]);
                    RuntimeExtensionMetaData declared = new RuntimeExtensionMetaData(
                            extension.name(), nameToken.value()[0],
                            extension.group(), extension.key()
                    );
                    runtimeExtensions.add(declared);
                }
            }
        }
    }

    private void handleGinExtensionElement(Element element) {
        List<? extends AnnotationMirror> annotationMirrors = element.getAnnotationMirrors();
        for (AnnotationMirror mirror : annotationMirrors) {
            final String annotationType = mirror.getAnnotationType().toString();
            if (annotationType.equals(GinExtension.class.getName())) {
                GinExtension comps = element.getAnnotation(GinExtension.class);
                final String module = comps.value();
                if (module != null && module.length() > 0) {
                    modules.add(module);
                }
                PackageElement packageElement = processingEnv.getElementUtils().getPackageOf(element);
                String fqn = packageElement.getQualifiedName().toString() + "." +
                        element.getSimpleName().toString();
                System.out.println("Components: " + fqn);
                discoveredExtensions.add(fqn);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void handleBeanFactoryElement(Element element) {
        List<? extends AnnotationMirror> annotationMirrors = element.getAnnotationMirrors();
        for (AnnotationMirror mirror : annotationMirrors) {
            final String annotationType = mirror.getAnnotationType().toString();
            if (annotationType.equals(BeanFactoryExtension.class.getName())) {
                PackageElement packageElement = processingEnv.getElementUtils().getPackageOf(element);
                String fqn = packageElement.getQualifiedName().toString() + "." +
                        element.getSimpleName().toString();
                System.out.println("Factory: " + fqn);
                discoveredBeanFactories.add(fqn);
            } else if (annotationType.equals("com.google.web.bindery.autobean.shared.AutoBeanFactory.Category")) {
                final Collection<? extends AnnotationValue> values = mirror.getElementValues().values();
                if (values.size() > 0) {
                    for (AnnotationValue categoryClass : (List<? extends AnnotationValue>) values.iterator().next()
                            .getValue()) {
                        categoryClasses.add(categoryClass.getValue().toString());
                    }
                }
            }
        }
    }

    private void handleSubsystemElement(Element element) {
        List<? extends AnnotationMirror> annotationMirrors = element.getAnnotationMirrors();
        for (AnnotationMirror mirror : annotationMirrors) {
            final String annotationType = mirror.getAnnotationType().toString();
            if (annotationType.equals(SubsystemExtension.class.getName())) {
                NameToken nameToken = element.getAnnotation(NameToken.class);
                SubsystemExtension subsystem = element.getAnnotation(SubsystemExtension.class);
                if (nameToken != null) {
                    System.out.println("Subsystem: " + subsystem.name() + " -> " + (nameToken.value()[0]));
                    SubsystemExtensionMetaData declared = new SubsystemExtensionMetaData(
                            subsystem.name(), nameToken.value()[0],
                            subsystem.group(), subsystem.key()
                    );

                    subsystemDeclararions.add(declared);
                    if (!nameTokens.add(nameToken.value()[0])) {
                        throw new RuntimeException("Duplicate name token '" + nameToken.value()[0] + "' declared on '"
                                + element.asType());
                    }
                }
            }
        }
    }

    private void writeFiles() throws Exception {
        writeGinjectorFile();
        writeBindingFile();
        writeBeanFactoryFile();
        writeSubsystemFile();
        writeAccessControlFile();
        writeSearchIndexFile();
        writeRuntimeFile();
        writeProxyConfigurations();
        writeVersionInfo();

        for (ModuleConfig moduleConfig : moduleConfigs) {
            moduleConfig.writeModuleFile(modules, gwtConfigProps);
        }
    }

    private void writeAccessControlFile() throws Exception {
        Map<String, Object> model = new HashMap<>();
        model.put("metaData", requiredResourcesDeclarations);
        model.put("operations", bootstrapOperations);

        JavaFileObject sourceFile = filer.createSourceFile(REQUIRED_RESOURCES_FILENAME);
        OutputStream output = sourceFile.openOutputStream();
        new TemplateProcessor().process(REQUIRED_RESOURCES_TEMPLATE, model, output);
        output.flush();
        output.close();
    }

    private void writeSearchIndexFile() throws IOException {
        Map<String, Object> model = new HashMap<>();
        model.put("metaData", searchIndexDeclarations);

        JavaFileObject sourceFile = filer.createSourceFile(SEARCH_INDEX_FILENAME);
        OutputStream output = sourceFile.openOutputStream();
        new TemplateProcessor().process(SEARCH_INDEX_TEMPLATE, model, output);
        output.flush();
        output.close();
    }

    private void writeGinjectorFile() throws Exception {

        Map<String, Object> model = new HashMap<>();
        model.put("extensions", discoveredExtensions);

        JavaFileObject sourceFile = filer.createSourceFile(EXTENSION_FILENAME);
        OutputStream output = sourceFile.openOutputStream();
        new TemplateProcessor().process(EXTENSION_TEMPLATE, model, output);
        output.flush();
        output.close();

    }

    private void writeBindingFile() throws Exception {
        JavaFileObject sourceFile = filer.createSourceFile(BINDING_FILENAME);
        Map<String, Object> model = new HashMap<>();
        model.put("extensions", discoveredBindings);

        OutputStream output = sourceFile.openOutputStream();
        new TemplateProcessor().process(BINDING_TEMPLATE, model, output);

        output.flush();
        output.close();
    }

    private void writeBeanFactoryFile() throws Exception {
        Map<String, Object> model = new HashMap<>();
        model.put("extensions", discoveredBeanFactories);
        model.put("categoryClasses", categoryClasses);

        JavaFileObject sourceFile = filer.createSourceFile(BEAN_FACTORY_FILENAME);
        OutputStream output = sourceFile.openOutputStream();
        new TemplateProcessor().process(BEAN_FACTORY_TEMPLATE, model, output);
        output.flush();
        output.close();
    }

    private void writeSubsystemFile() throws Exception {
        Map<String, Object> model = new HashMap<>();
        model.put("subsystemExtensions", subsystemDeclararions);

        JavaFileObject sourceFile = filer.createSourceFile(SUBSYSTEM_FILENAME);
        OutputStream output = sourceFile.openOutputStream();
        new TemplateProcessor().process(SUBSYSTEM_TEMPLATE, model, output);
        output.flush();
        output.close();
    }

    private void writeRuntimeFile() throws Exception {
        Map<String, Object> model = new HashMap<>();
        model.put("runtimeMenuItemExtensions", runtimeExtensions);

        JavaFileObject sourceFile = filer.createSourceFile(RUNTIME_FILENAME);
        OutputStream output = sourceFile.openOutputStream();
        new TemplateProcessor().process(RUNTIME_TEMPLATE, model, output);
        output.flush();
        output.close();
    }

    private void writeProxyConfigurations() {
        try {
            String devHost = gwtConfigProps.get("console.dev.host") != null ? gwtConfigProps
                    .get("console.dev.host") : "127.0.0.1";
            String devPort = gwtConfigProps.get("console.dev.port") != null ? gwtConfigProps
                    .get("console.dev.port") : "9990";

            Map<String, Object> model = new HashMap<>();
            model.put("devHost", devHost);
            model.put("devPort", devPort);

            FileObject sourceFile = filer.createResource(
                    StandardLocation.SOURCE_OUTPUT, "", "gwt-proxy.properties");
            OutputStream output1 = sourceFile.openOutputStream();

            FileObject sourceFile2 = filer.createResource(
                    StandardLocation.SOURCE_OUTPUT, "", "upload-proxy.properties");
            OutputStream output2 = sourceFile2.openOutputStream();

            FileObject sourceFile3 = filer.createResource(
                    StandardLocation.SOURCE_OUTPUT, "", "patch-proxy.properties");
            OutputStream output3 = sourceFile3.openOutputStream();

            FileObject sourceFile4 = filer.createResource(
                    StandardLocation.SOURCE_OUTPUT, "", "logout.properties");
            OutputStream output4 = sourceFile4.openOutputStream();

            new TemplateProcessor().process("gwt.proxy.tmpl", model, output1);
            new TemplateProcessor().process("gwt.proxy.upload.tmpl", model, output2);
            new TemplateProcessor().process("gwt.proxy.patch.tmpl", model, output3);
            new TemplateProcessor().process("gwt.proxy.logout.tmpl", model, output4);

            output1.close();
            output2.close();
            output3.close();
            output4.close();
        } catch (IOException e) {
            throw new RuntimeException("Failed to create file", e);
        }
    }

    private void writeVersionInfo() throws IOException {
        Map<String, String> options = processingEnv.getOptions();
        String version = options.containsKey("version") ? options.get("version") : "n/a";
        Map<String, Object> model = new HashMap<String, Object>();
        model.put("version", version);

        JavaFileObject sourceFile = filer.createSourceFile(VERSION_INFO_FILENAME);
        OutputStream output = sourceFile.openOutputStream();
        new TemplateProcessor().process(VERSION_INFO_TEMPLATE, model, output);
        output.flush();
        output.close();
    }
}
