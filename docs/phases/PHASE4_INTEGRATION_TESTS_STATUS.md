# Phase 4 Integration Tests - Estado Actual

**Fecha**: 9 de febrero de 2026  
**Estado**: En progreso - 8/12 tests pasando (67%)

## Resumen

Los tests de integración de Phase 4 (ConfigurationIntegrationTest) están parcialmente funcionando. Se han resuelto varios problemas críticos pero quedan 4 tests fallando.

## Problemas Resueltos ✅

### 1. Error de Compilación en TaskRepository
- **Problema**: Query HQL usaba `EXTRACT(EPOCH FROM ...)` incompatible con HQL
- **Solución**: Convertido a SQL nativo con `nativeQuery = true`
- **Resultado**: ✅ Código compila exitosamente

### 2. Error de Configuración JWT en Tests
- **Problema**: JWT secret con guiones causaba error de decodificación Base64
- **Problema 2**: JWT secret muy corto (< 512 bits para HS512)
- **Solución**: Actualizado a string alfanumérico de 88 caracteres
- **Resultado**: ✅ Tests ejecutan sin errores JWT

### 3. Error de Configuración de Cache
- **Problema**: `ConfigService` usa `@Cacheable` pero cache no configurado para tests
- **Error**: `IllegalArgumentException: Cannot find cache named 'tokenExpirationConfig'`
- **Solución**: Agregados todos los nombres de cache a `CacheConfig.java`:
  - tokenExpirationConfig
  - duplicateDetectionConfig
  - algorithmConfig
  - taskDistribution, mttr, heatmap, operatorMetrics
- **Resultado**: ✅ Cache funciona correctamente

## Tests Pasando (8/12) ✅

1. ✅ `shouldGetTokenExpirationConfigAsAdmin`
2. ✅ `shouldDenyTokenExpirationConfigAccessToNonAdmin`
3. ✅ `shouldRejectTokenExpirationConfigWithInvalidAccessToken`
4. ✅ `shouldRejectTokenExpirationConfigWithInvalidRefreshToken`
5. ✅ `shouldDenyDuplicateDetectionConfigAccessToNonAdmin`
6. ✅ `shouldRejectDuplicateDetectionConfigWithInvalidRadius`
7. ✅ `shouldRejectDuplicateDetectionConfigWithInvalidTimeWindow`
8. ✅ `shouldGetDuplicateDetectionConfigAsAdmin` (parcial)

## Tests Fallando (4/12) ⚠️

### 1. `shouldDenyTokenExpirationConfigAccessWithoutAuth`
- **Esperado**: 401 (Unauthorized)
- **Actual**: 403 (Forbidden)
- **Causa**: Configuración de Spring Security
- **Impacto**: Bajo - funcionalidad correcta, solo código de estado diferente

### 2. `shouldUpdateTokenExpirationConfigWithValidData`
- **Esperado**: Valor actualizado (30) en GET después de PUT
- **Actual**: Valor antiguo (15) retornado
- **Causa**: Interacción entre `@Transactional`, `@Commit` y cache
- **Problema**: Cache se puebla antes de que la transacción comita
- **Impacto**: Alto - afecta verificación de persistencia

### 3. `shouldUpdateDuplicateDetectionConfigWithValidData`
- **Esperado**: 200 OK
- **Actual**: 500 Internal Server Error
- **Causa**: Por determinar (posible error en servicio o validación)
- **Impacto**: Alto - funcionalidad no operativa

### 4. `shouldPreserveAlgorithmWeightsWhenUpdatingDuplicateDetection`
- **Esperado**: 200 OK
- **Actual**: 500 Internal Server Error
- **Causa**: Similar al test #3
- **Impacto**: Alto - funcionalidad no operativa

## Análisis Técnico

### Problema de Cache y Transacciones

El problema principal es la interacción entre:
1. `@Transactional` en tests (rollback automático)
2. `@Commit` para persistir cambios
3. `@Cacheable` en `ConfigService`
4. `@CacheEvict` en métodos de actualización

**Flujo problemático**:
```
1. Test hace PUT → actualiza config → @CacheEvict limpia cache
2. Transacción aún no comitada
3. Test hace GET → @Cacheable busca en cache (vacío)
4. Lee de BD → obtiene valor viejo (transacción no comitada)
5. Cachea valor viejo
6. Test falla porque esperaba valor nuevo
```

### Soluciones Intentadas

1. ❌ `@Transactional` a nivel de clase → rollback automático interfiere
2. ❌ `@Commit` en tests específicos → causa conflictos entre tests
3. ❌ `@DirtiesContext` → muy lento, recarga contexto completo
4. ⏳ Limpiar cache manualmente en `@BeforeEach` → implementado pero insuficiente

## Próximos Pasos

### Opción 1: Deshabilitar Cache en Tests (Recomendado)
```properties
# application-test.properties
spring.cache.type=none
```
- **Pros**: Simple, elimina complejidad de cache en tests
- **Contras**: No prueba comportamiento real de cache

### Opción 2: Usar `@DirtiesContext` Solo en Tests Problemáticos
```java
@Test
@Transactional
@Commit
@DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
void shouldUpdateTokenExpirationConfigWithValidData() { ... }
```
- **Pros**: Aísla tests problemáticos
- **Contras**: Tests más lentos

### Opción 3: Separar Tests de Lectura y Escritura
- Tests de lectura: `@Transactional` (rollback)
- Tests de escritura: Sin `@Transactional`, con cleanup manual
- **Pros**: Más control sobre transacciones
- **Contras**: Más complejo, requiere cleanup

### Opción 4: Investigar Errores 500
- Revisar logs detallados de los tests que fallan con 500
- Identificar causa raíz (validación, servicio, BD)
- Corregir error específico

## Recomendación

1. **Inmediato**: Investigar errores 500 en tests #3 y #4 (revisar logs con `-X`)
2. **Corto plazo**: Implementar Opción 1 (deshabilitar cache en tests)
3. **Largo plazo**: Considerar Opción 3 para tests más robustos

## Archivos Modificados

- ✅ `backend/src/main/java/com/urbanclean/repository/TaskRepository.java`
- ✅ `backend/src/test/resources/application-test.properties`
- ✅ `backend/src/main/java/com/urbanclean/config/CacheConfig.java`
- ✅ `backend/src/test/java/com/urbanclean/integration/ConfigurationIntegrationTest.java`

## Conclusión

**Progreso**: 67% de tests pasando (8/12)  
**Bloqueadores**: 4 tests fallando, 2 con errores 500 críticos  
**Tiempo estimado para resolución**: 1-2 horas  
**Prioridad**: Alta - necesario para completar Phase 4 al 100%
