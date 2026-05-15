package com.urbanclean.config;

import com.urbanclean.entity.*;
import com.urbanclean.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Seeds 500 demo reports and their corresponding tasks from Colombian cities.
 * Runs after DataInitializer (@Order 1) so users exist when this runs.
 * Only seeds when the reports table is empty.
 */
@Component
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final ReportRepository reportRepository;
    private final TaskRepository taskRepository;
    private final CountryRepository countryRepository;
    private final UserRepository userRepository;

    private static final GeometryFactory GEOMETRY_FACTORY =
            new GeometryFactory(new PrecisionModel(), 4326);

    @Override
    public void run(String... args) {
        if (reportRepository.count() > 0) {
            log.info("Reports already exist — skipping seed data.");
            return;
        }

        Country colombia = countryRepository.findByCode("COL").orElse(null);
        if (colombia == null) {
            log.warn("Colombia country not found in database — skipping report seed.");
            return;
        }

        User ciudadano = userRepository.findByUsername("ciudadano").orElse(null);
        User tecnico = userRepository.findByUsername("tecnico").orElse(null);

        if (ciudadano == null) {
            log.warn("ciudadano user not found — skipping report seed.");
            return;
        }

        log.info("Seeding 500 Colombian city reports and tasks...");
        List<Report> reports = buildReports(colombia, ciudadano);
        reportRepository.saveAll(reports);

        List<Task> tasks = buildTasks(reports, colombia, tecnico);
        taskRepository.saveAll(tasks);

        log.info("Seeded {} reports and {} tasks across Colombian cities.", reports.size(), tasks.size());
    }

    private List<Report> buildReports(Country colombia, User ciudadano) {
        List<Report> reports = new ArrayList<>(500);
        Random rnd = new Random(42);

        // [cityName, centerLat, centerLon, reportCount]
        Object[][] cities = {
            {"Bogotá",       4.7110,  -74.0721, 100},
            {"Medellín",     6.2518,  -75.5636,  80},
            {"Cali",         3.4516,  -76.5320,  70},
            {"Barranquilla", 10.9685, -74.7813,  50},
            {"Cartagena",    10.3910, -75.4794,  40},
            {"Bucaramanga",  7.1254,  -73.1198,  35},
            {"Ibagué",       4.4389,  -75.2322,  30},
            {"Pereira",      4.8143,  -75.6946,  25},
            {"Manizales",    5.0703,  -75.5138,  20},
            {"Guarne",       6.2765,  -75.4562,  20},
            {"Santa Marta",  11.2408, -74.1990,  15},
            {"Cúcuta",       7.8939,  -72.5078,  15},
        };

        String[] categories = {
            "BASURA_ACUMULADA",
            "CONTENEDOR_DANADO",
            "VERTIDO_ILEGAL",
            "LIMPIEZA_CALLE",
            "GRAFFITI",
            "OTRO",
        };

        String[][] descriptions = {
            {   // BASURA_ACUMULADA
                "Acumulación de basura doméstica sin recoger hace varios días",
                "Residuos sólidos amontonados en la esquina generando mal olor",
                "Bolsas de basura que obstruyen el andén y la calzada",
                "Residuos orgánicos en descomposición sobre la vía pública",
                "Basura doméstica frente a locales comerciales sin recolectar",
            },
            {   // CONTENEDOR_DANADO
                "Contenedor con la tapa rota y residuos derramados en la vía",
                "Recipiente de recolección volcado bloqueando el andén",
                "Caneca pública vandalizada e inutilizable",
                "Contenedor con daño estructural, necesita reemplazo urgente",
                "Papelera quemada y fuera de servicio",
            },
            {   // VERTIDO_ILEGAL
                "Escombros de construcción abandonados en el espacio público",
                "Aceite industrial vertido sobre el alcantarillado",
                "Residuos de demolición arrojados en lote baldío",
                "Desechos industriales cerca de la quebrada del sector",
                "Material de construcción depositado ilegalmente en zona verde",
            },
            {   // LIMPIEZA_CALLE
                "Calle con barro acumulado tras lluvias recientes",
                "Manchas de aceite en vía pública por accidente de tránsito",
                "Hojas y restos vegetales cubriendo el andén del parque",
                "Residuos de festividad que no fueron retirados por el servicio de aseo",
                "Vía principal con suciedad acumulada que requiere lavado",
            },
            {   // GRAFFITI
                "Pintadas vandálicas en fachada de edificio público",
                "Graffiti inapropiado en muro del colegio del barrio",
                "Vandalismo con pintura sobre señales de tránsito",
                "Paredes del parque cubiertas de graffiti no autorizado",
                "Pinturas vandálicas sobre puente peatonal",
            },
            {   // OTRO
                "Situación irregular de limpieza que requiere atención",
                "Problema de aseo en espacio público no clasificado",
                "Condición de limpieza deficiente en área comunitaria",
                "Incidencia relacionada con el servicio de aseo urbano",
                "Reporte general de limpieza en zona de alta afluencia",
            },
        };

        LocalDateTime now = LocalDateTime.now();

        for (Object[] city : cities) {
            String cityName = (String) city[0];
            double centerLat = (double) city[1];
            double centerLon = (double) city[2];
            int count = (int) city[3];

            for (int i = 0; i < count; i++) {
                double lat = centerLat + (rnd.nextDouble() - 0.5) * 0.06;
                double lon = centerLon + (rnd.nextDouble() - 0.5) * 0.06;
                Point location = GEOMETRY_FACTORY.createPoint(new Coordinate(lon, lat));

                int catIndex = rnd.nextInt(categories.length);
                String category = categories[catIndex];
                String description = descriptions[catIndex][rnd.nextInt(5)] + " en " + cityName + ".";

                LocalDateTime createdAt = now
                        .minusDays(rnd.nextInt(90))
                        .minusHours(rnd.nextInt(24))
                        .minusMinutes(rnd.nextInt(60));

                reports.add(Report.builder()
                        .location(location)
                        .category(category)
                        .description(description)
                        .isDuplicate(false)
                        .submitter(ciudadano)
                        .country(colombia)
                        .createdAt(createdAt)
                        .build());
            }
        }

        return reports;
    }

    private List<Task> buildTasks(List<Report> reports, Country colombia, User tecnico) {
        List<Task> tasks = new ArrayList<>(reports.size());
        Random rnd = new Random(42);

        // Priority base scores by category (10–90 scale)
        java.util.Map<String, Double> categoryScore = java.util.Map.of(
            "VERTIDO_ILEGAL",    85.0,
            "BASURA_ACUMULADA",  70.0,
            "CONTENEDOR_DANADO", 60.0,
            "LIMPIEZA_CALLE",    50.0,
            "GRAFFITI",          35.0,
            "OTRO",              20.0
        );

        TaskState[] states = TaskState.values();

        for (Report report : reports) {
            double base = categoryScore.getOrDefault(report.getCategory(), 50.0);
            double score = base + (rnd.nextDouble() - 0.5) * 20.0;
            score = Math.max(10.0, Math.min(90.0, score));
            BigDecimal priorityScore = BigDecimal.valueOf(Math.round(score * 100.0) / 100.0);

            // Distribute states: ~25% each across PENDIENTE, ASIGNADO, EN_PROGRESO, RESUELTO
            TaskState state = states[rnd.nextInt(4)];

            // Only assign an operator for states that need one
            User assignedOperator = (state != TaskState.PENDIENTE && tecnico != null) ? tecnico : null;

            tasks.add(Task.builder()
                    .primaryReport(report)
                    .location(report.getLocation())
                    .category(report.getCategory())
                    .state(state)
                    .priorityScore(priorityScore)
                    .duplicateCount(0)
                    .reopenCount(0)
                    .citizenApproved(false)
                    .assignedOperator(assignedOperator)
                    .country(colombia)
                    .build());
        }

        return tasks;
    }
}
