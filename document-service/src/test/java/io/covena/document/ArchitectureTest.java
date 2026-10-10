package io.covena.document;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.covena.document.domain.Document;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "io.covena.document", importOptions = ImportOption.DoNotIncludeTests.class)
@SuppressWarnings("unused")
class ArchitectureTest {
    @ArchTest
    static final ArchRule DOMAIN_IS_INDEPENDENT_OF_FRAMEWORKS_AND_ADAPTERS = noClasses()
        .that()
        .resideInAPackage("..domain..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("jakarta.persistence..", "org.springframework..", "..adapter..")
        .because("Domain classes should not depend on frameworks or adapters");

    @ArchTest
    static final ArchRule DOCUMENT_RESTORE_METHOD_CAN_BE_CALLED_FROM_ADAPTER_OUT_PERSISTENCE = methods()
        .that()
        .haveName("restore")
        .and()
        .areDeclaredIn(Document.class)
        .should()
        .onlyBeCalled()
        .byClassesThat()
        .resideInAPackage("..adapter.out.persistence..")
        .because("Document restore method can only be called from adapter out persistence package");
}
