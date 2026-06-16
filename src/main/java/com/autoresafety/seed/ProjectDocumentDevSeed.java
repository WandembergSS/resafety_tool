package com.autoresafety.seed;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.autoresafety.api.dto.project.ProjectDocumentDto;
import com.autoresafety.api.dto.project.SourceType;
import com.autoresafety.domain.Project;
import com.autoresafety.domain.ProjectStatus;
import com.autoresafety.persistence.ProjectRepository;
import com.autoresafety.service.ProjectDocumentService;

import io.quarkus.arc.profile.IfBuildProfile;
import io.quarkus.runtime.StartupEvent;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
@IfBuildProfile("dev")
public class ProjectDocumentDevSeed {

    @Inject
    ProjectRepository projectRepository;

    @Inject
    ProjectDocumentService projectDocumentService;

    @Transactional
    void seed(@Observes StartupEvent event) {
        Project project = projectRepository.findById(1L);
        if (project == null) {
            project = new Project();
            project.id = 1L;
            projectRepository.persist(project);
        }
        project.name = "Critical Railway Control System (CRCS) - Dissertation Case Study";
        project.description = "Dissertation Chapter 5 worked example: the full RESafety workflow (iStar4Safety + STPA) applied end to end to a Critical Railway Control System. All seven steps are filled and reviewed.";
        project.status = ProjectStatus.COMPLETED;

        ProjectDocumentDto.ProjectDto projectInfo = ProjectDocumentDto.ProjectDto.builder()
            .id(project.id)
            .name(project.name)
            .domain("Railway Transportation")
            .owner("Sarah")
            .description(project.description)
            .status(project.status)
            .currentStep(7)
            .createdAt(Instant.parse("2026-02-08T12:00:00Z"))
            .updatedAt(Instant.parse("2026-06-09T10:00:00Z"))
            .build();

        ProjectDocumentDto.Step1ScopeDto.GeneralSummaryDto summary = ProjectDocumentDto.Step1ScopeDto.GeneralSummaryDto.builder()
            .assumptions("Trains run on a signalled line under a moving-block scheme. Trackside block-occupancy detection and the train-to-wayside communication link are available under nominal conditions.")
            .systemDefinition("A Critical Railway Control System (CRCS) that supervises train movement, enforces safe separation and speed, and controls platform door operation.")
            .systemBoundary("Onboard train control, the wayside control centre, trackside occupancy sensors, and the platform door subsystem. Rolling-stock mechanics and civil infrastructure are outside the controlled boundary.")
            .outOfScope("Track construction and maintenance, rolling-stock propulsion design, and fare or passenger-information systems.")
            .build();

        ProjectDocumentDto.Step1ScopeDto step1 = ProjectDocumentDto.Step1ScopeDto.builder()
            .lastUpdatedBy("Sarah")
            .generalSummary(summary)
            .objectives("Prevent train-to-train collisions and platform-edge injuries by enforcing safe separation, safe speed, and safe door operation.")
            .resources(List.of(
            ProjectDocumentDto.Step1ScopeDto.ResourceDto.builder()
                .id(1L)
                .name("Railway signalling and operations manual")
                .category("documentation")
                .reference("DOC-RAIL-001")
                .sourceType(SourceType.MANUAL)
                .build(),
            ProjectDocumentDto.Step1ScopeDto.ResourceDto.builder()
                .id(2L)
                .name("Moving-block headway and movement-authority specification")
                .category("standard")
                .reference("STD-MA-014")
                .sourceType(SourceType.STANDARD)
                .build()
            ))
            .systemComponents(List.of(
            ProjectDocumentDto.Step1ScopeDto.SystemComponentDto.builder()
                .id(1L)
                .name("Train Control Controller")
                .description("Computes movement authority and commands braking to enforce safe separation and speed.")
                .build(),
            ProjectDocumentDto.Step1ScopeDto.SystemComponentDto.builder()
                .id(2L)
                .name("Door Controller")
                .description("Commands platform and train door opening and closing, interlocked with motion and berth status.")
                .build(),
            ProjectDocumentDto.Step1ScopeDto.SystemComponentDto.builder()
                .id(3L)
                .name("Operator")
                .description("Supervises line operation from the control centre and issues movement authority.")
                .build(),
            ProjectDocumentDto.Step1ScopeDto.SystemComponentDto.builder()
                .id(4L)
                .name("Block Occupancy Sensor")
                .description("Trackside detection that reports whether each track block is occupied.")
                .build(),
            ProjectDocumentDto.Step1ScopeDto.SystemComponentDto.builder()
                .id(5L)
                .name("Braking Actuator")
                .description("Applies service and emergency braking on command from the Train Control Controller.")
                .build()
            ))
            .accidents(List.of(
            ProjectDocumentDto.Step1ScopeDto.AccidentDto.builder()
                .id(1L)
                .code("A-1")
                .description("Train-to-train collision between consecutive trains on the same line.")
                .build(),
            ProjectDocumentDto.Step1ScopeDto.AccidentDto.builder()
                .id(2L)
                .code("A-2")
                .description("Passenger injury at the platform edge during boarding or alighting.")
                .build()
            ))
            .hazards(List.of(
            ProjectDocumentDto.Step1ScopeDto.HazardDto.builder()
                .id(1L)
                .code("H-1")
                .description("Two trains occupy the same track block at the same time (loss of safe separation).")
                .linkedAccidents(List.of("A-1"))
                .build(),
            ProjectDocumentDto.Step1ScopeDto.HazardDto.builder()
                .id(2L)
                .code("H-2")
                .description("Doors open while the train is in motion or not berthed at a platform.")
                .linkedAccidents(List.of("A-2"))
                .build(),
            ProjectDocumentDto.Step1ScopeDto.HazardDto.builder()
                .id(3L)
                .code("H-3")
                .description("A train travels faster than the speed authorised for its current track section.")
                .linkedAccidents(List.of("A-1"))
                .build()
            ))
            .safetyConstraints(List.of(
            ProjectDocumentDto.Step1ScopeDto.SafetyConstraintDto.builder()
                .id(1L)
                .code("SC-1")
                .statement("The CRCS shall keep at least one clear block of separation between consecutive trains on the same line.")
                .linkedHazards(List.of("H-1"))
                .build(),
            ProjectDocumentDto.Step1ScopeDto.SafetyConstraintDto.builder()
                .id(2L)
                .code("SC-2")
                .statement("The CRCS shall not allow doors to open unless the train is stationary and berthed at a platform.")
                .linkedHazards(List.of("H-2"))
                .build(),
            ProjectDocumentDto.Step1ScopeDto.SafetyConstraintDto.builder()
                .id(3L)
                .code("SC-3")
                .statement("The CRCS shall keep each train within the speed authorised for its current track section.")
                .linkedHazards(List.of("H-3"))
                .build()
            ))
            .responsibilities(List.of(
            ProjectDocumentDto.Step1ScopeDto.ResponsibilityDto.builder()
                .id(1L)
                .component("Train Control Controller")
                .responsibility("Command braking to preserve safe separation and safe speed.")
                .linkedConstraints(List.of("SC-1", "SC-3"))
                .build(),
            ProjectDocumentDto.Step1ScopeDto.ResponsibilityDto.builder()
                .id(2L)
                .component("Door Controller")
                .responsibility("Inhibit door opening unless the train is stopped and berthed.")
                .linkedConstraints(List.of("SC-2"))
                .build()
            ))
            .artefacts(List.of(
            ProjectDocumentDto.Step1ScopeDto.ArtefactDto.builder()
                .id(1L)
                .name("CRCS system architecture overview")
                .purpose("Components, controllers, and interfaces of the railway control system")
                .reference("ARCH-CRCS-001")
                .build(),
            ProjectDocumentDto.Step1ScopeDto.ArtefactDto.builder()
                .id(2L)
                .name("3. System Safety Control Structure")
                .purpose("Controllers, controlled processes, and control actions")
                .reference("CSS-CRCS-003")
                .build()
            ))
            .build();

        ProjectDocumentDto.Step2IstarDto step2 = ProjectDocumentDto.Step2IstarDto.builder()
            .modelName("Critical Railway Control System - iStar4Safety model")
            .actors(List.of(
                ProjectDocumentDto.Step2IstarDto.ActorDto.builder()
                    .id("1")
                    .name("Train Control Controller")
                    .type("Agent")
                    .x(220.0)
                    .y(180.0)
                    .intentionalElements(List.of(
                        ProjectDocumentDto.Step2IstarDto.IntentionalElementDto.builder()
                            .id("ie-g-1")
                            .name("Ensure safe train movement")
                            .type("Goal")
                            .x(240.0)
                            .y(110.0)
                            .build(),
                        ProjectDocumentDto.Step2IstarDto.IntentionalElementDto.builder()
                            .id("ie-sg-1")
                            .name("Maintain safe distance")
                            .type("SafetyGoal")
                            .safetyGoalKind("Safety Constraint")
                            .x(240.0)
                            .y(240.0)
                            .obstructs(List.of())
                            .build(),
                        ProjectDocumentDto.Step2IstarDto.IntentionalElementDto.builder()
                            .id("ie-hz-1")
                            .name("Two trains occupy the same block")
                            .type("Hazard")
                            .x(430.0)
                            .y(240.0)
                            .obstructs(List.of("ie-sg-1"))
                            .build(),
                        ProjectDocumentDto.Step2IstarDto.IntentionalElementDto.builder()
                            .id("ie-t-1")
                            .name("Command braking")
                            .type("Task")
                            .x(240.0)
                            .y(330.0)
                            .build()
                    ))
                    .responsibilities(List.of("Ensure safe train movement", "Maintain safe distance", "Command braking"))
                    .build(),
                ProjectDocumentDto.Step2IstarDto.ActorDto.builder()
                    .id("2")
                    .name("Operator")
                    .type("Role")
                    .x(640.0)
                    .y(180.0)
                    .intentionalElements(List.of(
                        ProjectDocumentDto.Step2IstarDto.IntentionalElementDto.builder()
                            .id("ie-g-2")
                            .name("Supervise line operation")
                            .type("Goal")
                            .x(660.0)
                            .y(110.0)
                            .build(),
                        ProjectDocumentDto.Step2IstarDto.IntentionalElementDto.builder()
                            .id("ie-t-2")
                            .name("Issue movement authority")
                            .type("Task")
                            .x(660.0)
                            .y(300.0)
                            .build()
                    ))
                    .responsibilities(List.of("Supervise line operation", "Issue movement authority"))
                    .build()
            ))
            .dependencies(List.of(
                ProjectDocumentDto.Step2IstarDto.DependencyDto.builder()
                    .id("dep-1")
                    .depender("Operator")
                    .dependerElement("Supervise line operation")
                    .dependee("Train Control Controller")
                    .dependeeElement("Maintain safe distance")
                    .x(440.0)
                    .y(200.0)
                    .dependum(ProjectDocumentDto.Step2IstarDto.DependumDto.builder()
                        .id("dum-1")
                        .name("Maintain safe distance")
                        .type("Goal")
                        .build())
                    .build()
            ))
            .goalLinks(List.of(
                ProjectDocumentDto.Step2IstarDto.GoalLinkDto.builder()
                    .id(1L)
                    .fromActor("Train Control Controller")
                    .goal("Maintain safe distance")
                    .linkType("SafetyGoal")
                    .build(),
                ProjectDocumentDto.Step2IstarDto.GoalLinkDto.builder()
                    .id(2L)
                    .fromActor("Train Control Controller")
                    .goal("Two trains occupy the same block obstructs Maintain safe distance")
                    .linkType("obstructs")
                    .build(),
                ProjectDocumentDto.Step2IstarDto.GoalLinkDto.builder()
                    .id(3L)
                    .fromActor("Operator")
                    .goal("Maintain safe distance")
                    .linkType("dependency")
                    .build()
            ))
            .build();

        ProjectDocumentDto.Step3ControlStructureDto step3 = ProjectDocumentDto.Step3ControlStructureDto.builder()
            .controlActions(List.of(
                ProjectDocumentDto.Step3ControlStructureDto.ControlActionDto.builder()
                    .id(1L)
                    .controller("Train Control Controller")
                    .action("Command service braking")
                    .controlledProcess("Train braking system")
                    .feedback("Block occupancy and speed")
                    .build(),
                ProjectDocumentDto.Step3ControlStructureDto.ControlActionDto.builder()
                    .id(2L)
                    .controller("Train Control Controller")
                    .action("Command emergency braking")
                    .controlledProcess("Train braking system")
                    .feedback("Block occupancy and speed")
                    .build(),
                ProjectDocumentDto.Step3ControlStructureDto.ControlActionDto.builder()
                    .id(3L)
                    .controller("Door Controller")
                    .action("Command doors open")
                    .controlledProcess("Train doors")
                    .feedback("Door position and berth status")
                    .build(),
                ProjectDocumentDto.Step3ControlStructureDto.ControlActionDto.builder()
                    .id(4L)
                    .controller("Operator")
                    .action("Issue movement authority")
                    .controlledProcess("Train Control Controller")
                    .feedback("Line status display")
                    .build()
            ))
            .feedbackLoops(List.of(
                ProjectDocumentDto.Step3ControlStructureDto.FeedbackLoopDto.builder()
                    .id(1L)
                    .source("Block Occupancy Sensor")
                    .destination("Train Control Controller")
                    .signal("Block occupancy status")
                    .latency("<200ms")
                    .build(),
                ProjectDocumentDto.Step3ControlStructureDto.FeedbackLoopDto.builder()
                    .id(2L)
                    .source("Train doors")
                    .destination("Door Controller")
                    .signal("Door position and berth status")
                    .latency("<100ms")
                    .build()
            ))
            .build();

        ProjectDocumentDto.Step4UcasDto step4 = ProjectDocumentDto.Step4UcasDto.builder()
            .ucas(List.of(
                ProjectDocumentDto.Step4UcasDto.UcaDto.builder()
                    .id(1L)
                    .ref("UCA-01")
                    .controlActionRef("CA-1")
                    .sourceActor("Train Control Controller")
                    .targetActor("Train braking system")
                    .controller("Train Control Controller")
                    .controlAction("Command service braking")
                    .controlledProcess("Train braking system")
                    .hazard("H-1")
                    .category("Not provided")
                    .context("when the preceding block is occupied")
                    .consequence("Loss of safe separation that can lead to a train-to-train collision")
                    .rationale("The Train Control Controller does not command braking when the preceding block is occupied.")
                    .hazardRefs(List.of("H-1"))
                    .responsibilityId("R-1")
                    .safetyConstraintId("SC-1")
                    .build(),
                ProjectDocumentDto.Step4UcasDto.UcaDto.builder()
                    .id(2L)
                    .ref("UCA-02")
                    .controlActionRef("CA-3")
                    .sourceActor("Door Controller")
                    .targetActor("Train doors")
                    .controller("Door Controller")
                    .controlAction("Command doors open")
                    .controlledProcess("Train doors")
                    .hazard("H-2")
                    .category("Provided")
                    .context("while the train is still in motion")
                    .consequence("Passenger fall or injury at the platform edge")
                    .rationale("The Door Controller commands the doors to open while the train is still moving.")
                    .hazardRefs(List.of("H-2"))
                    .responsibilityId("R-2")
                    .safetyConstraintId("SC-2")
                    .build(),
                ProjectDocumentDto.Step4UcasDto.UcaDto.builder()
                    .id(3L)
                    .ref("UCA-03")
                    .controlActionRef("CA-1")
                    .sourceActor("Train Control Controller")
                    .targetActor("Train braking system")
                    .controller("Train Control Controller")
                    .controlAction("Command service braking")
                    .controlledProcess("Train braking system")
                    .hazard("H-3")
                    .category("Incorrect timing")
                    .context("too late when entering a lower-speed section")
                    .consequence("The train overspeeds within the restricted section")
                    .rationale("The Train Control Controller commands braking too late when the train enters a lower-speed section.")
                    .hazardRefs(List.of("H-3"))
                    .responsibilityId("R-1")
                    .safetyConstraintId("SC-3")
                    .build()
            ))
            .hazardousConditions(List.of(
                ProjectDocumentDto.Step4UcasDto.HazardousConditionDto.builder()
                    .id(1L)
                    .ref("HC-01")
                    .description("Block-occupancy feedback is stale, so the process model of the controller wrongly indicates that the next block is clear.")
                    .context("Degraded train-to-wayside communication")
                    .consequence("The controller withholds braking and separation is lost")
                    .hazardRefs(List.of("H-1"))
                    .build()
            ))
            .controllerConstraints(List.of(
                ProjectDocumentDto.Step4UcasDto.ControllerConstraintDto.builder()
                    .id(1L)
                    .sourceUcaHc("UCA-01")
                    .constraintId("CC-01")
                    .constraintStatement("The Train Control Controller must command braking when the preceding block is occupied.")
                    .build(),
                ProjectDocumentDto.Step4UcasDto.ControllerConstraintDto.builder()
                    .id(2L)
                    .sourceUcaHc("UCA-02")
                    .constraintId("CC-02")
                    .constraintStatement("The Door Controller must not command the doors to open while the train is in motion.")
                    .build(),
                ProjectDocumentDto.Step4UcasDto.ControllerConstraintDto.builder()
                    .id(3L)
                    .sourceUcaHc("UCA-03")
                    .constraintId("CC-03")
                    .constraintStatement("The Train Control Controller must command braking in time to remain within the authorised speed for the current section.")
                    .build(),
                ProjectDocumentDto.Step4UcasDto.ControllerConstraintDto.builder()
                    .id(4L)
                    .sourceUcaHc("HC-01")
                    .constraintId("CC-04")
                    .constraintStatement("The Train Control Controller must treat stale block-occupancy feedback as block occupied and command braking.")
                    .build()
            ))
            .build();

        ProjectDocumentDto.Step5ControllerConstraintsDto step5 = ProjectDocumentDto.Step5ControllerConstraintsDto.builder()
            .constraints(List.of(
                ProjectDocumentDto.Step5ControllerConstraintsDto.ConstraintDto.builder()
                    .id(1L)
                    .ucaRef("UCA-01")
                    .constraint("The Train Control Controller shall command service braking whenever the preceding block is occupied or its occupancy cannot be confirmed.")
                    .enforcementMechanism("Safe-separation guard in the train control loop that brakes on occupied or unconfirmed blocks")
                    .status("defined")
                    .build(),
                ProjectDocumentDto.Step5ControllerConstraintsDto.ConstraintDto.builder()
                    .id(2L)
                    .ucaRef("UCA-02")
                    .constraint("The Door Controller shall keep the doors closed unless a validated stopped-and-berthed signal is present.")
                    .enforcementMechanism("Door interlock tied to the validated motion and berth status")
                    .status("defined")
                    .build(),
                ProjectDocumentDto.Step5ControllerConstraintsDto.ConstraintDto.builder()
                    .id(3L)
                    .ucaRef("UCA-03")
                    .constraint("The Train Control Controller shall start braking before the train reaches a lower-speed section so the authorised speed is met at the boundary.")
                    .enforcementMechanism("Speed-supervision curve that anticipates section speed transitions")
                    .status("defined")
                    .build()
            ))
            .build();

        ProjectDocumentDto.Step5InformationDto step5Information = ProjectDocumentDto.Step5InformationDto.builder()
            .lossScenarios(List.of(
                ProjectDocumentDto.Step5InformationDto.LossScenarioDto.builder()
                    .id("LS-01")
                    .description("Braking is omitted because degraded train-to-wayside communication delivers stale block-occupancy data, so the process model of the controller wrongly assumes the preceding block is clear and the train continues toward an occupied block.")
                    .associatedUnsafeBehaviorIds(List.of("UCA-01", "HC-01"))
                    .sourceRationale("Derived from UCA-01 and HC-01 (loss of safe separation).")
                    .build(),
                ProjectDocumentDto.Step5InformationDto.LossScenarioDto.builder()
                    .id("LS-02")
                    .description("Doors are commanded open while the train is still moving because the door controller relies on an out-of-date stopped-and-berthed status from a delayed feedback signal.")
                    .associatedUnsafeBehaviorIds(List.of("UCA-02"))
                    .sourceRationale("Derived from UCA-02 (door operation in motion).")
                    .build(),
                ProjectDocumentDto.Step5InformationDto.LossScenarioDto.builder()
                    .id("LS-03")
                    .description("Braking is applied too late on entry to a lower-speed section because the speed-limit transition is detected only after the train has already entered the restricted block.")
                    .associatedUnsafeBehaviorIds(List.of("UCA-03"))
                    .sourceRationale("Derived from UCA-03 (overspeed in restricted section).")
                    .build()
            ))
            .safetyRequirements(List.of(
                ProjectDocumentDto.Step5InformationDto.SafetyRequirementDto.builder()
                    .id("SR-01")
                    .description("The CRCS shall command service braking within 200 ms whenever block-occupancy feedback indicates, or fails to confirm within the timeout, that the preceding block is not clear; stale feedback shall be treated as occupied.")
                    .addressedLossScenarioIds(List.of("LS-01"))
                    .build(),
                ProjectDocumentDto.Step5InformationDto.SafetyRequirementDto.builder()
                    .id("SR-02")
                    .description("The CRCS shall inhibit any door-open command unless a validated train-stopped-and-berthed signal is present, and shall re-verify that signal within 100 ms of issuing the command.")
                    .addressedLossScenarioIds(List.of("LS-02"))
                    .build(),
                ProjectDocumentDto.Step5InformationDto.SafetyRequirementDto.builder()
                    .id("SR-03")
                    .description("The CRCS shall begin braking before a train crosses into a lower-speed section so that the authorised speed is met at the section boundary.")
                    .addressedLossScenarioIds(List.of("LS-03"))
                    .build()
            ))
            .build();

        ProjectDocumentDto.Step6LossScenariosDto step6 = ProjectDocumentDto.Step6LossScenariosDto.builder()
            .lossScenarios(List.of(
                ProjectDocumentDto.Step6LossScenariosDto.LossScenarioDto.builder()
                    .id(1L)
                    .uca("UCA-01")
                    .hazard("H-1")
                    .outcome("The train enters an occupied block, leading to a train-to-train collision.")
                    .severity("Critical")
                    .mitigations(List.of("Treat stale occupancy data as occupied", "Add independent secondary block detection"))
                    .status("open")
                    .build(),
                ProjectDocumentDto.Step6LossScenariosDto.LossScenarioDto.builder()
                    .id(2L)
                    .uca("UCA-02")
                    .hazard("H-2")
                    .outcome("Doors open while moving, leading to a passenger fall at the platform edge.")
                    .severity("Major")
                    .mitigations(List.of("Hard door interlock on validated berth status", "Re-verify berth status after the open command"))
                    .status("open")
                    .build(),
                ProjectDocumentDto.Step6LossScenariosDto.LossScenarioDto.builder()
                    .id(3L)
                    .uca("UCA-03")
                    .hazard("H-3")
                    .outcome("The train overspeeds in a restricted section.")
                    .severity("Major")
                    .mitigations(List.of("Anticipatory speed supervision", "Automatic braking at the section boundary"))
                    .status("mitigated")
                    .build()
            ))
            .safetyRequirements(List.of(
                ProjectDocumentDto.Step6LossScenariosDto.SafetyRequirementDto.builder()
                    .id(1L)
                    .title("SR-01 Command braking on an unconfirmed clear block")
                    .linkedScenario(1L)
                    .category("functional")
                    .owner("Signalling Safety Team")
                    .dueDate(LocalDate.of(2026, 7, 15))
                    .status("planned")
                    .build(),
                ProjectDocumentDto.Step6LossScenariosDto.SafetyRequirementDto.builder()
                    .id(2L)
                    .title("SR-02 Inhibit door opening unless stopped and berthed")
                    .linkedScenario(2L)
                    .category("functional")
                    .owner("Door Systems Team")
                    .dueDate(LocalDate.of(2026, 7, 22))
                    .status("planned")
                    .build(),
                ProjectDocumentDto.Step6LossScenariosDto.SafetyRequirementDto.builder()
                    .id(3L)
                    .title("SR-03 Anticipatory braking for speed transitions")
                    .linkedScenario(3L)
                    .category("performance")
                    .owner("Train Control Team")
                    .dueDate(LocalDate.of(2026, 7, 29))
                    .status("in-progress")
                    .build()
            ))
            .build();

        ProjectDocumentDto.Step7ModelUpdateDto step7 = ProjectDocumentDto.Step7ModelUpdateDto.builder()
            .updatedSteps(ProjectDocumentDto.Step7ModelUpdateDto.UpdatedStepsDto.builder()
                .step1(true)
                .step2(true)
                .step3(true)
                .step4(true)
                .step5(true)
                .build())
            .modelChanges(List.of(
                ProjectDocumentDto.Step7ModelUpdateDto.ModelChangeDto.builder()
                    .id(1L)
                    .area("Train Control")
                    .change("Added a stale-feedback-means-occupied guard so braking is commanded whenever block occupancy cannot be confirmed.")
                    .driver("UCA-01 and HC-01")
                    .impact("Removes the loss-of-separation scenario LS-01")
                    .status("implemented")
                    .evidence(List.of("SIM-CRCS-101", "TEST-LOG-244"))
                    .build(),
                ProjectDocumentDto.Step7ModelUpdateDto.ModelChangeDto.builder()
                    .id(2L)
                    .area("Door Control")
                    .change("Tied the door-open command to a validated stopped-and-berthed interlock with re-verification.")
                    .driver("UCA-02")
                    .impact("Removes the door-in-motion scenario LS-02")
                    .status("implemented")
                    .evidence(List.of("SIM-CRCS-118"))
                    .build()
            ))
            .validationTasks(List.of(
                ProjectDocumentDto.Step7ModelUpdateDto.ValidationTaskDto.builder()
                    .id(1L)
                    .name("Hardware-in-the-loop safe-separation test")
                    .owner("Verification and Validation Team")
                    .dueDate("2026-07-20")
                    .channel("rail-test-lab")
                    .status("queued")
                    .build(),
                ProjectDocumentDto.Step7ModelUpdateDto.ValidationTaskDto.builder()
                    .id(2L)
                    .name("Door interlock regression test")
                    .owner("Door Systems Team")
                    .dueDate("2026-07-27")
                    .channel("rail-test-lab")
                    .status("queued")
                    .build()
            ))
            .integrationNotes(List.of(
                ProjectDocumentDto.Step7ModelUpdateDto.IntegrationNoteDto.builder()
                    .id(1L)
                    .summary("Signalling and door-control interlocks aligned across the updated control structure.")
                    .createdOn("2026-06-09")
                    .author("Systems Engineer")
                    .actionItems(List.of("Run the full regression suite", "Update the operations playbook"))
                    .build()
            ))
            .build();

        ProjectDocumentDto document = ProjectDocumentDto.builder()
            .project(projectInfo)
            .step1Scope(step1)
            .step2Istar(step2)
            .step3ControlStructure(step3)
            .step4Ucas(step4)
            .step5ControllerConstraints(step5)
            .step5Information(step5Information)
            .step6LossScenarios(step6)
            .step7ModelUpdate(step7)
            .build();

        projectDocumentService.saveOrUpdate(project.id, document);
    }
}
