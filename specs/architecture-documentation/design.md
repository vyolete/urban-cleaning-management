# Design Document - Architecture Documentation Generation

## Overview

This design document outlines the approach for generating comprehensive architectural documentation for the Urban Cleaning Management System through systematic codebase analysis. The documentation generation process will extract architectural patterns, component relationships, and system behaviors directly from source code to produce academic-quality technical documentation suitable for a Master's Thesis (TFM).

The system being documented is a full-stack web application built with:
- **Backend**: Spring Boot 3.2.2 (Java 17), PostgreSQL 15 + PostGIS 3.3
- **Frontend**: React 18, Leaflet for maps
- **Deployment**: Docker + Docker Compose
- **Security**: JWT authentication, Spring Security, BCrypt password hashing

The documentation will follow UML standards and include all seven architectural views: Use Case, Logical, Data Model, MVC, Process, Deployment, and Implementation.

## Architecture

### Documentation Generation Approach

The architecture documentation will be generated through a multi-phase analysis process:

1. **Code Discovery Phase**: Scan and catalog all source files
2. **Entity Analysis Phase**: Extract domain model from JPA entities
3. **API Analysis Phase**: Extract endpoints and operations from controllers
4. **Service Analysis Phase**: Extract business logic and workflows from services
5. **Integration Analysis Phase**: Trace component interactions and dependencies
6. **Deployment Analysis Phase**: Extract deployment topology from Docker configs
7. **Synthesis Phase**: Generate UML diagrams and documentation

### Analysis Strategy

**Static Code Analysis**: The primary analysis method will be static code inspection:
- Parse Java source files to extract class structures, annotations, and relationships
- Parse React JSX files to identify UI components and state management
- Parse configuration files (pom.xml, package.json, docker-compose.yml)
- Trace method calls and dependencies through import statements

**Pattern Recognition**: Identify architectural patterns from code structure:
- MVC pattern from package organization (controller/, service/, entity/)
- Repository pattern from Spring Data JPA repositories
- Event-driven patterns from ApplicationEventPublisher usage
- State machine patterns from enum-based state transitions

**Relationship Extraction**: Build component relationship graphs:
- Entity relationships from JPA annotations (@OneToMany, @ManyToOne, etc.)
- Service dependencies from constructor injection
- API routes from @RequestMapping annotations
- Frontend-backend integration from API service calls

## Components and Interfaces

### Documentation Generator Components

#### 1. Code Scanner
**Responsibility**: Discover and catalog all source files in the codebase

**Inputs**:
- Root directory path (backend/, frontend/)
- File extension filters (.java, .jsx, .yml)

**Outputs**:
- Categorized file inventory (entities, controllers, services, components)

**Algorithm**:
```
function scanCodebase(rootPath):
    files = recursivelyListFiles(rootPath)
    categorizedFiles = {
        entities: [],
        controllers: [],
        services: [],
        repositories: [],
        dtos: [],
        components: [],
        configs: []
    }
    
    for file in files:
        category = categorizeFile(file.path, file.content)
        categorizedFiles[category].append(file)
    
    return categorizedFiles
```

#### 2. Entity Analyzer
**Responsibility**: Extract domain model from JPA entities

**Inputs**:
- Entity source files (*.java files with @Entity annotation)

**Outputs**:
- Entity metadata (name, attributes, types, constraints)
- Relationship metadata (type, cardinality, foreign keys)
- Index and constraint information

**Algorithm**:
```
function analyzeEntity(entityFile):
    entityMetadata = {
        name: extractClassName(entityFile),
        tableName: extractTableName(entityFile),
        attributes: [],
        relationships: [],
        indexes: []
    }
    
    for field in entityFile.fields:
        if hasAnnotation(field, "@Id"):
            entityMetadata.primaryKey = field.name
        
        if hasAnnotation(field, "@Column"):
            attribute = extractColumnMetadata(field)
            entityMetadata.attributes.append(attribute)
        
        if hasAnnotation(field, "@ManyToOne", "@OneToMany", "@ManyToMany"):
            relationship = extractRelationshipMetadata(field)
            entityMetadata.relationships.append(relationship)
    
    return entityMetadata
```

#### 3. API Analyzer
**Responsibility**: Extract REST API structure from controllers

**Inputs**:
- Controller source files (*Controller.java)

**Outputs**:
- Endpoint inventory (HTTP method, path, parameters, responses)
- Actor-to-endpoint mappings (based on @PreAuthorize annotations)
- Use case extraction from endpoint operations

**Algorithm**:
```
function analyzeController(controllerFile):
    apiMetadata = {
        basePath: extractRequestMapping(controllerFile),
        endpoints: []
    }
    
    for method in controllerFile.methods:
        if hasAnnotation(method, "@GetMapping", "@PostMapping", etc.):
            endpoint = {
                httpMethod: extractHttpMethod(method),
                path: extractPath(method),
                parameters: extractParameters(method),
                returnType: extractReturnType(method),
                security: extractSecurityAnnotation(method),
                description: extractJavadoc(method)
            }
            apiMetadata.endpoints.append(endpoint)
    
    return apiMetadata
```

#### 4. Service Analyzer
**Responsibility**: Extract business logic and workflows from service layer

**Inputs**:
- Service source files (*Service.java)

**Outputs**:
- Service method inventory with signatures
- Business process flows (method call sequences)
- Transaction boundaries (@Transactional annotations)
- Event publishing patterns

**Algorithm**:
```
function analyzeService(serviceFile):
    serviceMetadata = {
        name: extractClassName(serviceFile),
        dependencies: extractConstructorDependencies(serviceFile),
        methods: []
    }
    
    for method in serviceFile.methods:
        methodMetadata = {
            name: method.name,
            parameters: extractParameters(method),
            returnType: extractReturnType(method),
            transactional: hasAnnotation(method, "@Transactional"),
            callSequence: extractMethodCalls(method.body),
            eventsPublished: extractEventPublishing(method.body)
        }
        serviceMetadata.methods.append(methodMetadata)
    
    return serviceMetadata
```

#### 5. Sequence Diagram Generator
**Responsibility**: Generate sequence diagrams from method call traces

**Inputs**:
- Service method metadata with call sequences
- Controller-to-service mappings

**Outputs**:
- Mermaid sequence diagram syntax
- Textual description of interaction flow

**Algorithm**:
```
function generateSequenceDiagram(workflow):
    participants = identifyParticipants(workflow)
    interactions = []
    
    for step in workflow.steps:
        interaction = {
            from: step.caller,
            to: step.callee,
            message: step.methodName,
            returnValue: step.returnType
        }
        interactions.append(interaction)
    
    mermaidSyntax = convertToMermaid(participants, interactions)
    return mermaidSyntax
```

#### 6. Class Diagram Generator
**Responsibility**: Generate class diagram from entity and DTO analysis

**Inputs**:
- Entity metadata
- DTO metadata
- Service metadata

**Outputs**:
- Mermaid class diagram syntax
- Textual description of class relationships

**Algorithm**:
```
function generateClassDiagram(entities, dtos, services):
    classes = []
    relationships = []
    
    for entity in entities:
        classNode = {
            name: entity.name,
            attributes: entity.attributes,
            methods: []
        }
        classes.append(classNode)
        
        for relationship in entity.relationships:
            relationshipEdge = {
                from: entity.name,
                to: relationship.targetEntity,
                type: relationship.type,
                cardinality: relationship.cardinality
            }
            relationships.append(relationshipEdge)
    
    mermaidSyntax = convertToMermaid(classes, relationships)
    return mermaidSyntax
```

#### 7. State Diagram Generator
**Responsibility**: Generate state diagrams for entities with state machines

**Inputs**:
- Entity metadata with enum state fields
- Service methods that perform state transitions

**Outputs**:
- Mermaid state diagram syntax
- State transition rules documentation

**Algorithm**:
```
function generateStateDiagram(entity, transitionMethods):
    states = extractEnumValues(entity.stateField)
    transitions = []
    
    for method in transitionMethods:
        transition = extractStateTransition(method.body)
        if transition:
            transitions.append({
                from: transition.currentState,
                to: transition.newState,
                trigger: method.name,
                guard: transition.validationCondition
            })
    
    mermaidSyntax = convertToMermaid(states, transitions)
    return mermaidSyntax
```

#### 8. Deployment Analyzer
**Responsibility**: Extract deployment architecture from Docker configuration

**Inputs**:
- docker-compose.yml
- Dockerfile files

**Outputs**:
- Deployment component inventory
- Service dependencies and network topology
- Volume and port mappings

**Algorithm**:
```
function analyzeDeployment(dockerComposeFile):
    deploymentMetadata = {
        services: [],
        networks: [],
        volumes: []
    }
    
    for service in dockerComposeFile.services:
        serviceMetadata = {
            name: service.name,
            image: service.image,
            ports: service.ports,
            environment: service.environment,
            dependsOn: service.depends_on,
            volumes: service.volumes
        }
        deploymentMetadata.services.append(serviceMetadata)
    
    return deploymentMetadata
```

#### 9. Documentation Synthesizer
**Responsibility**: Combine all analysis results into structured documentation

**Inputs**:
- All analyzer outputs
- Generated diagrams

**Outputs**:
- Complete Markdown documentation file(s)

**Algorithm**:
```
function synthesizeDocumentation(analysisResults):
    documentation = {
        useCaseView: generateUseCaseView(analysisResults.api, analysisResults.services),
        logicalView: generateLogicalView(analysisResults.sequences, analysisResults.classes),
        dataModel: generateDataModel(analysisResults.entities),
        mvcView: generateMVCView(analysisResults.controllers, analysisResults.components),
        processView: generateProcessView(analysisResults.workflows),
        deploymentView: generateDeploymentView(analysisResults.deployment),
        implementationView: generateImplementationView(analysisResults.packages)
    }
    
    markdownOutput = convertToMarkdown(documentation)
    return markdownOutput
```

### Component Interfaces

**ICodeScanner**:
```java
interface ICodeScanner {
    CategorizedFiles scanCodebase(String rootPath, List<String> extensions);
}
```

**IEntityAnalyzer**:
```java
interface IEntityAnalyzer {
    EntityMetadata analyzeEntity(SourceFile entityFile);
    List<EntityMetadata> analyzeAllEntities(List<SourceFile> entityFiles);
}
```

**IAPIAnalyzer**:
```java
interface IAPIAnalyzer {
    APIMetadata analyzeController(SourceFile controllerFile);
    List<Endpoint> extractEndpoints(SourceFile controllerFile);
}
```

**IDiagramGenerator**:
```java
interface IDiagramGenerator {
    String generateSequenceDiagram(Workflow workflow);
    String generateClassDiagram(List<EntityMetadata> entities);
    String generateStateDiagram(EntityMetadata entity, List<TransitionMethod> methods);
}
```

## Data Models

### Analysis Metadata Models

#### EntityMetadata
```
EntityMetadata {
    name: String
    tableName: String
    primaryKey: AttributeMetadata
    attributes: List<AttributeMetadata>
    relationships: List<RelationshipMetadata>
    indexes: List<IndexMetadata>
    stateField: AttributeMetadata (optional)
}
```

#### AttributeMetadata
```
AttributeMetadata {
    name: String
    type: String
    columnName: String
    nullable: Boolean
    unique: Boolean
    length: Integer (optional)
    precision: Integer (optional)
    scale: Integer (optional)
}
```

#### RelationshipMetadata
```
RelationshipMetadata {
    type: RelationshipType (ONE_TO_ONE, ONE_TO_MANY, MANY_TO_ONE, MANY_TO_MANY)
    sourceEntity: String
    targetEntity: String
    mappedBy: String (optional)
    joinColumn: String (optional)
    cardinality: String
}
```

#### EndpointMetadata
```
EndpointMetadata {
    httpMethod: HTTPMethod (GET, POST, PUT, PATCH, DELETE)
    path: String
    parameters: List<ParameterMetadata>
    requestBody: TypeMetadata (optional)
    responseType: TypeMetadata
    securityRoles: List<String>
    description: String
}
```

#### ServiceMethodMetadata
```
ServiceMethodMetadata {
    name: String
    parameters: List<ParameterMetadata>
    returnType: TypeMetadata
    transactional: Boolean
    callSequence: List<MethodCall>
    eventsPublished: List<EventMetadata>
}
```

#### WorkflowMetadata
```
WorkflowMetadata {
    name: String
    entryPoint: String (controller method)
    steps: List<WorkflowStep>
    participants: List<String> (components involved)
}
```

#### WorkflowStep
```
WorkflowStep {
    sequenceNumber: Integer
    caller: String (component name)
    callee: String (component name)
    methodName: String
    parameters: List<String>
    returnType: String
}
```

### Documentation Output Models

#### UseCaseSpecification
```
UseCaseSpecification {
    id: String
    name: String
    actor: String
    description: String
    preconditions: List<String>
    mainFlow: List<String>
    alternativeFlows: List<AlternativeFlow>
    postconditions: List<String>
    relatedEndpoints: List<String>
}
```

#### DiagramSpecification
```
DiagramSpecification {
    type: DiagramType (SEQUENCE, CLASS, STATE, DEPLOYMENT, COMPONENT, ACTIVITY)
    title: String
    mermaidSyntax: String
    description: String
    participants: List<String> (for sequence diagrams)
}
```

## Correctness Properties


*A property is a characteristic or behavior that should hold true across all valid executions of a system—essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Actor Identification Completeness
*For any* authentication role defined in the codebase (UserRole enum) or security annotation (@PreAuthorize), the system should identify and document it as an actor in the use case view.

**Validates: Requirements 1.1**

### Property 2: Use Case Extraction Completeness
*For any* controller method with a request mapping annotation (@GetMapping, @PostMapping, etc.), the system should generate a corresponding use case specification.

**Validates: Requirements 1.2**

### Property 3: Use Case Specification Completeness
*For any* generated use case, the specification should include all required fields: name, actor, description, main flow, alternative flows, preconditions, and postconditions.

**Validates: Requirements 1.3**

### Property 4: Class Diagram Completeness
*For any* class annotated with @Entity, @RestController, @Service, @Repository, or located in dto/ package, the system should include it in the class diagram.

**Validates: Requirements 2.2**

### Property 5: State Diagram Generation
*For any* entity with an enum-typed field representing state, the system should generate a state diagram showing all possible states and transitions.

**Validates: Requirements 2.3**

### Property 6: Collaboration Diagram Generation
*For any* service method that calls multiple other services or repositories, the system should include it in a collaboration diagram showing component interactions.

**Validates: Requirements 2.4**

### Property 7: Entity Extraction and Documentation Completeness
*For any* JPA entity in the codebase, the system should:
- Extract the entity with its table name
- Document all attributes with their data types and constraints
- Identify primary keys (@Id annotations)
- Identify foreign keys (@JoinColumn annotations)
- Identify indexes (@Index annotations)
- Document all relationships (@OneToMany, @ManyToOne, @ManyToMany) with correct cardinality

**Validates: Requirements 3.1, 3.2, 3.3, 3.4**

### Property 8: MVC Component Identification Completeness
*For any* file in the codebase, the system should correctly classify it as:
- View component (if .jsx file in frontend/src/components/ or frontend/src/pages/)
- Controller component (if Java class with @RestController annotation)
- Model component (if Java class with @Entity annotation or in dto/ package)

**Validates: Requirements 4.1, 4.2, 4.3**

### Property 9: Business Process Documentation Completeness
*For any* service method that orchestrates multiple operations (calls multiple services/repositories), the system should:
- Identify it as a business process
- Document the process flow with entry and exit points
- Create a process model showing workflow execution
- Document process dependencies (injected services)

**Validates: Requirements 5.1, 5.2, 5.4, 5.5**

### Property 10: Deployment Configuration Completeness
*For any* service defined in docker-compose.yml, the system should:
- Identify it as a deployment component
- Document its type (container, database, etc.)
- Extract and document dependencies (depends_on relationships)
- Document environment requirements (environment variables)
- Document port mappings and volumes

**Validates: Requirements 6.1, 6.2, 6.3, 6.4**

### Property 11: Package Structure Component Diagram
*For any* Java package in the backend or directory in the frontend, the system should include it in the component diagram showing the module structure.

**Validates: Requirements 7.1**

### Property 12: Service Dependency Documentation
*For any* service class with constructor-injected dependencies, the system should document the interfaces between that service and its dependencies.

**Validates: Requirements 7.2**

### Property 13: External Dependency Identification
*For any* dependency declared in pom.xml or package.json, the system should identify and document it as an external dependency.

**Validates: Requirements 7.4**

### Property 14: Design Pattern Documentation
*For any* detected design pattern (Repository, MVC, Event-Driven, State Machine), the system should document it in the design decisions section.

**Validates: Requirements 8.1**

### Property 15: Technology Stack Identification
*For any* technology used in the system (identifiable from pom.xml, package.json, annotations, or imports), the system should identify and document it, including:
- Spring Boot (from pom.xml parent)
- React (from package.json dependencies)
- PostgreSQL (from docker-compose.yml)
- PostGIS (from Hibernate Spatial dependency)
- JWT (from jjwt dependencies)
- BCrypt (from Spring Security usage)

**Validates: Requirements 8.2, 8.3, 8.4**

### Property 16: Code-Based Accuracy
*For any* documented feature, component, or behavior, the system should:
- Have a corresponding source file reference
- Extract information exclusively from source files
- Validate the feature against actual code
- Not assume functionality not present in the codebase

**Validates: Requirements 10.1, 10.2, 10.4**

### Property 17: Ambiguity Handling
*For any* functionality that cannot be clearly determined from code analysis, the system should mark it as "implementation-dependent" rather than making assumptions.

**Validates: Requirements 10.3**

### Property 18: Documentation Cross-Referencing
*For any* related documentation sections (e.g., entity in Data Model and class in Class Diagram), the system should include cross-references linking them together.

**Validates: Requirements 11.3**

### Property 19: Diagram Legend Inclusion
*For any* generated diagram that uses symbols or notation, the system should include a legend or key explaining the symbols.

**Validates: Requirements 12.4**

### Property 20: Markdown Format Compliance
*For any* generated documentation file, the system should:
- Use valid Markdown syntax
- Use proper heading hierarchy (h1 > h2 > h3, no skipped levels)
- Format code examples with syntax highlighting (language-tagged code fences)
- Use Markdown tables for structured data

**Validates: Requirements 13.1, 13.2, 13.3, 13.4**

## Error Handling

### Analysis Errors

**Missing Source Files**:
- **Error**: Required source files not found (e.g., no entities, no controllers)
- **Handling**: Log warning, document what was found, note missing components in output
- **Recovery**: Continue with available files, generate partial documentation

**Parse Errors**:
- **Error**: Unable to parse source file (syntax errors, encoding issues)
- **Handling**: Log error with file path, skip file, continue with other files
- **Recovery**: Document that file was skipped due to parse error

**Annotation Extraction Failures**:
- **Error**: Unable to extract expected annotations from source
- **Handling**: Log warning, use fallback heuristics (e.g., class name patterns)
- **Recovery**: Document with lower confidence, mark as "inferred"

### Diagram Generation Errors

**Complex Relationship Cycles**:
- **Error**: Circular dependencies that make diagram unreadable
- **Handling**: Detect cycles, simplify diagram by breaking cycles at weakest links
- **Recovery**: Document full relationships in text, show simplified diagram

**Too Many Components**:
- **Error**: Diagram would be too large to be readable
- **Handling**: Split into multiple diagrams by logical grouping
- **Recovery**: Create overview diagram + detailed sub-diagrams

**Mermaid Syntax Limitations**:
- **Error**: Relationship type not supported by Mermaid
- **Handling**: Use closest supported notation, document actual relationship in text
- **Recovery**: Add explanatory note in diagram description

### Documentation Synthesis Errors

**Inconsistent Information**:
- **Error**: Conflicting information from different sources (e.g., entity name vs table name)
- **Handling**: Prefer code over configuration, document both if conflict exists
- **Recovery**: Add note explaining discrepancy

**Missing Required Sections**:
- **Error**: Unable to generate required architectural view
- **Handling**: Create section with explanation of why it couldn't be generated
- **Recovery**: Document what information was missing from codebase

## Testing Strategy

### Unit Testing Approach

**Component Testing**:
- Test each analyzer component independently with sample source files
- Verify correct extraction of metadata from known code patterns
- Test error handling with malformed input

**Example Unit Tests**:
```java
@Test
void testEntityAnalyzer_extractsBasicEntity() {
    String entitySource = """
        @Entity
        @Table(name = "users")
        public class User {
            @Id
            private UUID id;
            
            @Column(nullable = false)
            private String username;
        }
    """;
    
    EntityMetadata metadata = entityAnalyzer.analyzeEntity(entitySource);
    
    assertEquals("User", metadata.getName());
    assertEquals("users", metadata.getTableName());
    assertEquals("id", metadata.getPrimaryKey().getName());
    assertEquals(1, metadata.getAttributes().size());
}

@Test
void testAPIAnalyzer_extractsEndpointWithSecurity() {
    String controllerSource = """
        @RestController
        @RequestMapping("/api/tasks")
        public class TaskController {
            @PreAuthorize("hasRole('TECNICO')")
            @GetMapping
            public List<Task> getTasks() {
                return taskService.getAllTasks();
            }
        }
    """;
    
    APIMetadata metadata = apiAnalyzer.analyzeController(controllerSource);
    
    assertEquals("/api/tasks", metadata.getBasePath());
    assertEquals(1, metadata.getEndpoints().size());
    assertEquals("TECNICO", metadata.getEndpoints().get(0).getSecurityRoles().get(0));
}
```

### Property-Based Testing Approach

Property-based tests will verify universal properties across all code analysis scenarios using randomized inputs and comprehensive coverage.

**Test Configuration**:
- Minimum 100 iterations per property test
- Use actual codebase files as test corpus
- Generate variations of code patterns

**Property Test Examples**:

**Property Test 1: Entity Extraction Completeness**
```java
@Property(trials = 100)
@Tag("Feature: architecture-documentation, Property 7: Entity Extraction and Documentation Completeness")
void allEntitiesAreExtracted(@ForAll("entityFiles") List<File> entityFiles) {
    // Given: A set of entity files from the codebase
    List<EntityMetadata> extractedEntities = entityAnalyzer.analyzeAllEntities(entityFiles);
    
    // Then: Every file with @Entity annotation should be extracted
    long expectedCount = entityFiles.stream()
        .filter(file -> containsAnnotation(file, "@Entity"))
        .count();
    
    assertEquals(expectedCount, extractedEntities.size());
    
    // And: Each entity should have all required metadata
    for (EntityMetadata entity : extractedEntities) {
        assertNotNull(entity.getName());
        assertNotNull(entity.getTableName());
        assertNotNull(entity.getPrimaryKey());
        assertFalse(entity.getAttributes().isEmpty());
    }
}

@Provide
Arbitrary<List<File>> entityFiles() {
    // Provide actual entity files from backend/src/main/java/com/urbanclean/entity/
    return Arbitraries.of(
        new File("backend/src/main/java/com/urbanclean/entity/User.java"),
        new File("backend/src/main/java/com/urbanclean/entity/Report.java"),
        new File("backend/src/main/java/com/urbanclean/entity/Task.java")
        // ... all entity files
    ).list().ofMinSize(1);
}
```

**Property Test 2: Use Case Extraction Completeness**
```java
@Property(trials = 100)
@Tag("Feature: architecture-documentation, Property 2: Use Case Extraction Completeness")
void allControllerMethodsGenerateUseCases(@ForAll("controllerFiles") List<File> controllerFiles) {
    // Given: A set of controller files from the codebase
    List<UseCaseSpecification> useCases = useCaseGenerator.generateUseCases(controllerFiles);
    
    // Then: Every controller method with request mapping should have a use case
    long expectedCount = controllerFiles.stream()
        .flatMap(file -> extractRequestMappingMethods(file).stream())
        .count();
    
    assertEquals(expectedCount, useCases.size());
    
    // And: Each use case should have all required fields
    for (UseCaseSpecification useCase : useCases) {
        assertNotNull(useCase.getName());
        assertNotNull(useCase.getActor());
        assertNotNull(useCase.getDescription());
        assertFalse(useCase.getMainFlow().isEmpty());
    }
}
```

**Property Test 3: Code-Based Accuracy**
```java
@Property(trials = 100)
@Tag("Feature: architecture-documentation, Property 16: Code-Based Accuracy")
void allDocumentedFeaturesHaveCodeReferences(@ForAll("documentationSections") DocumentationSection section) {
    // Given: Any section of generated documentation
    List<DocumentedFeature> features = section.getFeatures();
    
    // Then: Every documented feature should have a source file reference
    for (DocumentedFeature feature : features) {
        assertNotNull(feature.getSourceFileReference());
        assertTrue(feature.getSourceFileReference().exists());
        
        // And: The feature should be verifiable in the source code
        assertTrue(verifyFeatureInCode(feature, feature.getSourceFileReference()));
    }
}
```

**Property Test 4: Markdown Format Compliance**
```java
@Property(trials = 100)
@Tag("Feature: architecture-documentation, Property 20: Markdown Format Compliance")
void generatedDocumentationIsValidMarkdown(@ForAll("documentationFiles") File docFile) {
    // Given: Any generated documentation file
    String content = readFile(docFile);
    
    // Then: The file should be valid Markdown
    assertTrue(isValidMarkdown(content));
    
    // And: Heading hierarchy should be proper (no skipped levels)
    List<Integer> headingLevels = extractHeadingLevels(content);
    for (int i = 1; i < headingLevels.size(); i++) {
        int diff = headingLevels.get(i) - headingLevels.get(i-1);
        assertTrue(diff <= 1, "Heading level skipped: " + headingLevels);
    }
    
    // And: Code blocks should have language tags
    List<String> codeBlocks = extractCodeBlocks(content);
    for (String block : codeBlocks) {
        assertTrue(hasLanguageTag(block), "Code block missing language tag");
    }
}
```

**Property Test 5: Relationship Cardinality Correctness**
```java
@Property(trials = 100)
@Tag("Feature: architecture-documentation, Property 7: Entity Extraction and Documentation Completeness")
void relationshipCardinalityMatchesAnnotations(@ForAll("entityFiles") File entityFile) {
    // Given: An entity file with relationship annotations
    EntityMetadata entity = entityAnalyzer.analyzeEntity(entityFile);
    
    // Then: For each relationship, cardinality should match the annotation type
    for (RelationshipMetadata rel : entity.getRelationships()) {
        String expectedCardinality = deriveCardinalityFromType(rel.getType());
        assertEquals(expectedCardinality, rel.getCardinality());
    }
}

private String deriveCardinalityFromType(RelationshipType type) {
    return switch (type) {
        case ONE_TO_ONE -> "1:1";
        case ONE_TO_MANY -> "1:N";
        case MANY_TO_ONE -> "N:1";
        case MANY_TO_MANY -> "N:M";
    };
}
```

### Integration Testing

**End-to-End Documentation Generation**:
- Run full analysis on actual codebase
- Verify all 7 architectural views are generated
- Validate cross-references between sections
- Check diagram syntax validity

**Example Integration Test**:
```java
@Test
void testFullDocumentationGeneration() {
    // Given: The actual Urban Cleaning Management codebase
    String backendPath = "backend/src/main/java";
    String frontendPath = "frontend/src";
    String dockerPath = "docker";
    
    // When: Full documentation is generated
    ArchitectureDocumentation doc = documentationGenerator.generate(
        backendPath, frontendPath, dockerPath
    );
    
    // Then: All 7 views should be present
    assertNotNull(doc.getUseCaseView());
    assertNotNull(doc.getLogicalView());
    assertNotNull(doc.getDataModel());
    assertNotNull(doc.getMvcView());
    assertNotNull(doc.getProcessView());
    assertNotNull(doc.getDeploymentView());
    assertNotNull(doc.getImplementationView());
    
    // And: Key components should be documented
    assertTrue(doc.getDataModel().getEntities().size() >= 10);
    assertTrue(doc.getUseCaseView().getUseCases().size() >= 15);
    assertTrue(doc.getLogicalView().getSequenceDiagrams().size() >= 10);
    
    // And: Output should be valid Markdown
    String markdown = doc.toMarkdown();
    assertTrue(isValidMarkdown(markdown));
}
```

### Manual Validation

**Academic Quality Review**:
- Review generated documentation for academic language
- Verify UML terminology usage
- Check diagram clarity and readability
- Validate completeness against codebase

**Accuracy Verification**:
- Spot-check documented features against source code
- Verify no assumed functionality
- Validate relationship cardinalities
- Check state transition accuracy

## Implementation Notes

### Technology Choices

**Analysis Tools**:
- **JavaParser**: For parsing Java source files and extracting AST
- **Regex + String Parsing**: For extracting React component structure
- **YAML Parser**: For parsing docker-compose.yml
- **XML Parser**: For parsing pom.xml

**Diagram Generation**:
- **Mermaid**: Primary diagram syntax (widely supported, text-based)
- **PlantUML**: Alternative for complex diagrams if needed

**Output Format**:
- **Markdown**: Primary documentation format
- **File Structure**: Single comprehensive file or organized multi-file structure

### Performance Considerations

**Caching**:
- Cache parsed source files to avoid re-parsing
- Cache extracted metadata for reuse across views

**Parallel Processing**:
- Analyze files in parallel where possible
- Independent view generation can be parallelized

**Incremental Generation**:
- Support regenerating specific views without full analysis
- Track file changes to minimize re-analysis

### Extensibility

**Plugin Architecture**:
- Support custom analyzers for additional languages
- Support custom diagram generators
- Support custom documentation templates

**Configuration**:
- Configurable complexity thresholds (e.g., for activity diagrams)
- Configurable diagram limits (e.g., max components per diagram)
- Configurable output format preferences
