-- Re-seeds project id = 1 as the dissertation worked example:
-- the Critical Railway Control System (CRCS) case study (Chapter 5).
-- All seven RESafety steps are filled and marked as reviewed, and the
-- project is left in the COMPLETED state so it is easy to identify on the
-- dashboard. Idempotent: both rows use ON CONFLICT ... DO UPDATE.

INSERT INTO project (id, name, description, status, created_at, updated_at)
VALUES (
  1,
  'Critical Railway Control System (CRCS) - Dissertation Case Study',
  'Dissertation Chapter 5 worked example: the full RESafety workflow (iStar4Safety + STPA) applied end to end to a Critical Railway Control System. All seven steps are filled and reviewed.',
  'COMPLETED',
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP
)
ON CONFLICT (id) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description,
    status = EXCLUDED.status,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO project_document (project_id, document, created_at, updated_at)
VALUES (
  1,
  '{
    "project": {
      "id": 1,
      "name": "Critical Railway Control System (CRCS) - Dissertation Case Study",
      "domain": "Railway Transportation",
      "owner": "Sarah",
      "description": "Dissertation Chapter 5 worked example: the full RESafety workflow (iStar4Safety + STPA) applied end to end to a Critical Railway Control System. All seven steps are filled and reviewed.",
      "status": "COMPLETED",
      "currentStep": 7,
      "createdAt": "2026-02-08T12:00:00Z",
      "updatedAt": "2026-06-09T10:00:00Z"
    },
    "step1Scope": {
      "lastUpdatedBy": "Sarah",
      "generalSummary": {
        "assumptions": "Trains run on a signalled line under a moving-block scheme. Trackside block-occupancy detection and the train-to-wayside communication link are available under nominal conditions.",
        "systemDefinition": "A Critical Railway Control System (CRCS) that supervises train movement, enforces safe separation and speed, and controls platform door operation.",
        "systemBoundary": "Onboard train control, the wayside control centre, trackside occupancy sensors, and the platform door subsystem. Rolling-stock mechanics and civil infrastructure are outside the controlled boundary.",
        "outOfScope": "Track construction and maintenance, rolling-stock propulsion design, and fare or passenger-information systems."
      },
      "objectives": "Prevent train-to-train collisions and platform-edge injuries by enforcing safe separation, safe speed, and safe door operation.",
      "resources": [
        {
          "id": 1,
          "name": "Railway signalling and operations manual",
          "category": "documentation",
          "reference": "DOC-RAIL-001",
          "sourceType": "manual"
        },
        {
          "id": 2,
          "name": "Moving-block headway and movement-authority specification",
          "category": "standard",
          "reference": "STD-MA-014",
          "sourceType": "standard"
        }
      ],
      "systemComponents": [
        {
          "id": 1,
          "name": "Train Control Controller",
          "description": "Computes movement authority and commands braking to enforce safe separation and speed."
        },
        {
          "id": 2,
          "name": "Door Controller",
          "description": "Commands platform and train door opening and closing, interlocked with motion and berth status."
        },
        {
          "id": 3,
          "name": "Operator",
          "description": "Supervises line operation from the control centre and issues movement authority."
        },
        {
          "id": 4,
          "name": "Block Occupancy Sensor",
          "description": "Trackside detection that reports whether each track block is occupied."
        },
        {
          "id": 5,
          "name": "Braking Actuator",
          "description": "Applies service and emergency braking on command from the Train Control Controller."
        }
      ],
      "accidents": [
        {
          "id": 1,
          "code": "A-1",
          "description": "Train-to-train collision between consecutive trains on the same line."
        },
        {
          "id": 2,
          "code": "A-2",
          "description": "Passenger injury at the platform edge during boarding or alighting."
        }
      ],
      "hazards": [
        {
          "id": 1,
          "code": "H-1",
          "description": "Two trains occupy the same track block at the same time (loss of safe separation).",
          "linkedAccidents": ["A-1"]
        },
        {
          "id": 2,
          "code": "H-2",
          "description": "Doors open while the train is in motion or not berthed at a platform.",
          "linkedAccidents": ["A-2"]
        },
        {
          "id": 3,
          "code": "H-3",
          "description": "A train travels faster than the speed authorised for its current track section.",
          "linkedAccidents": ["A-1"]
        }
      ],
      "safetyConstraints": [
        {
          "id": 1,
          "code": "SC-1",
          "statement": "The CRCS shall keep at least one clear block of separation between consecutive trains on the same line.",
          "linkedHazards": ["H-1"]
        },
        {
          "id": 2,
          "code": "SC-2",
          "statement": "The CRCS shall not allow doors to open unless the train is stationary and berthed at a platform.",
          "linkedHazards": ["H-2"]
        },
        {
          "id": 3,
          "code": "SC-3",
          "statement": "The CRCS shall keep each train within the speed authorised for its current track section.",
          "linkedHazards": ["H-3"]
        }
      ],
      "responsibilities": [
        {
          "id": 1,
          "component": "Train Control Controller",
          "responsibility": "Command braking to preserve safe separation and safe speed.",
          "linkedConstraints": ["SC-1", "SC-3"]
        },
        {
          "id": 2,
          "component": "Door Controller",
          "responsibility": "Inhibit door opening unless the train is stopped and berthed.",
          "linkedConstraints": ["SC-2"]
        }
      ],
      "artefacts": [
        {
          "id": 1,
          "name": "CRCS system architecture overview",
          "purpose": "Components, controllers, and interfaces of the railway control system",
          "reference": "ARCH-CRCS-001"
        },
        {
          "id": 2,
          "name": "3. System Safety Control Structure",
          "purpose": "Controllers, controlled processes, and control actions",
          "reference": "CSS-CRCS-003"
        }
      ]
    },
    "step2Istar": {
      "modelName": "Critical Railway Control System - iStar4Safety model",
      "actors": [
        {
          "id": "1",
          "name": "Train Control Controller",
          "type": "Agent",
          "x": 220,
          "y": 180,
          "intentionalElements": [
            {
              "id": "ie-g-1",
              "name": "Ensure safe train movement",
              "type": "Goal",
              "x": 240,
              "y": 110
            },
            {
              "id": "ie-sg-1",
              "name": "Maintain safe distance",
              "type": "SafetyGoal",
              "safetyGoalKind": "Safety Constraint",
              "x": 240,
              "y": 240,
              "obstructs": []
            },
            {
              "id": "ie-hz-1",
              "name": "Two trains occupy the same block",
              "type": "Hazard",
              "x": 430,
              "y": 240,
              "obstructs": ["ie-sg-1"]
            },
            {
              "id": "ie-t-1",
              "name": "Command braking",
              "type": "Task",
              "x": 240,
              "y": 330
            }
          ],
          "responsibilities": ["Ensure safe train movement", "Maintain safe distance", "Command braking"]
        },
        {
          "id": "2",
          "name": "Operator",
          "type": "Role",
          "x": 640,
          "y": 180,
          "intentionalElements": [
            {
              "id": "ie-g-2",
              "name": "Supervise line operation",
              "type": "Goal",
              "x": 660,
              "y": 110
            },
            {
              "id": "ie-t-2",
              "name": "Issue movement authority",
              "type": "Task",
              "x": 660,
              "y": 300
            }
          ],
          "responsibilities": ["Supervise line operation", "Issue movement authority"]
        }
      ],
      "dependencies": [
        {
          "id": "dep-1",
          "depender": "Operator",
          "dependerElement": "Supervise line operation",
          "dependee": "Train Control Controller",
          "dependeeElement": "Maintain safe distance",
          "x": 440,
          "y": 200,
          "dependum": {
            "id": "dum-1",
            "name": "Maintain safe distance",
            "type": "Goal"
          }
        }
      ],
      "goalLinks": [
        {
          "id": 1,
          "fromActor": "Train Control Controller",
          "goal": "Maintain safe distance",
          "linkType": "SafetyGoal"
        },
        {
          "id": 2,
          "fromActor": "Train Control Controller",
          "goal": "Two trains occupy the same block obstructs Maintain safe distance",
          "linkType": "obstructs"
        },
        {
          "id": 3,
          "fromActor": "Operator",
          "goal": "Maintain safe distance",
          "linkType": "dependency"
        }
      ]
    },
    "step3ControlStructure": {
      "controlActions": [
        {
          "id": 1,
          "controller": "Train Control Controller",
          "action": "Command service braking",
          "controlledProcess": "Train braking system",
          "feedback": "Block occupancy and speed"
        },
        {
          "id": 2,
          "controller": "Train Control Controller",
          "action": "Command emergency braking",
          "controlledProcess": "Train braking system",
          "feedback": "Block occupancy and speed"
        },
        {
          "id": 3,
          "controller": "Door Controller",
          "action": "Command doors open",
          "controlledProcess": "Train doors",
          "feedback": "Door position and berth status"
        },
        {
          "id": 4,
          "controller": "Operator",
          "action": "Issue movement authority",
          "controlledProcess": "Train Control Controller",
          "feedback": "Line status display"
        }
      ],
      "feedbackLoops": [
        {
          "id": 1,
          "source": "Block Occupancy Sensor",
          "destination": "Train Control Controller",
          "signal": "Block occupancy status",
          "latency": "<200ms"
        },
        {
          "id": 2,
          "source": "Train doors",
          "destination": "Door Controller",
          "signal": "Door position and berth status",
          "latency": "<100ms"
        }
      ]
    },
    "step4Ucas": {
      "ucas": [
        {
          "id": 1,
          "ref": "UCA-01",
          "controlActionRef": "CA-1",
          "sourceActor": "Train Control Controller",
          "targetActor": "Train braking system",
          "controller": "Train Control Controller",
          "controlAction": "Command service braking",
          "controlledProcess": "Train braking system",
          "hazard": "H-1",
          "category": "Not provided",
          "context": "when the preceding block is occupied",
          "consequence": "Loss of safe separation that can lead to a train-to-train collision",
          "rationale": "The Train Control Controller does not command braking when the preceding block is occupied.",
          "hazardRefs": ["H-1"],
          "responsibilityId": "R-1",
          "safetyConstraintId": "SC-1"
        },
        {
          "id": 2,
          "ref": "UCA-02",
          "controlActionRef": "CA-3",
          "sourceActor": "Door Controller",
          "targetActor": "Train doors",
          "controller": "Door Controller",
          "controlAction": "Command doors open",
          "controlledProcess": "Train doors",
          "hazard": "H-2",
          "category": "Provided",
          "context": "while the train is still in motion",
          "consequence": "Passenger fall or injury at the platform edge",
          "rationale": "The Door Controller commands the doors to open while the train is still moving.",
          "hazardRefs": ["H-2"],
          "responsibilityId": "R-2",
          "safetyConstraintId": "SC-2"
        },
        {
          "id": 3,
          "ref": "UCA-03",
          "controlActionRef": "CA-1",
          "sourceActor": "Train Control Controller",
          "targetActor": "Train braking system",
          "controller": "Train Control Controller",
          "controlAction": "Command service braking",
          "controlledProcess": "Train braking system",
          "hazard": "H-3",
          "category": "Incorrect timing",
          "context": "too late when entering a lower-speed section",
          "consequence": "The train overspeeds within the restricted section",
          "rationale": "The Train Control Controller commands braking too late when the train enters a lower-speed section.",
          "hazardRefs": ["H-3"],
          "responsibilityId": "R-1",
          "safetyConstraintId": "SC-3"
        }
      ],
      "hazardousConditions": [
        {
          "id": 1,
          "ref": "HC-01",
          "description": "Block-occupancy feedback is stale, so the process model of the controller wrongly indicates that the next block is clear.",
          "context": "Degraded train-to-wayside communication",
          "consequence": "The controller withholds braking and separation is lost",
          "hazardRefs": ["H-1"]
        }
      ],
      "controllerConstraints": [
        {
          "id": 1,
          "sourceUcaHc": "UCA-01",
          "constraintId": "CC-01",
          "constraintStatement": "The Train Control Controller must command braking when the preceding block is occupied."
        },
        {
          "id": 2,
          "sourceUcaHc": "UCA-02",
          "constraintId": "CC-02",
          "constraintStatement": "The Door Controller must not command the doors to open while the train is in motion."
        },
        {
          "id": 3,
          "sourceUcaHc": "UCA-03",
          "constraintId": "CC-03",
          "constraintStatement": "The Train Control Controller must command braking in time to remain within the authorised speed for the current section."
        },
        {
          "id": 4,
          "sourceUcaHc": "HC-01",
          "constraintId": "CC-04",
          "constraintStatement": "The Train Control Controller must treat stale block-occupancy feedback as block occupied and command braking."
        }
      ]
    },
    "step5ControllerConstraints": {
      "constraints": [
        {
          "id": 1,
          "ucaRef": "UCA-01",
          "constraint": "The Train Control Controller shall command service braking whenever the preceding block is occupied or its occupancy cannot be confirmed.",
          "enforcementMechanism": "Safe-separation guard in the train control loop that brakes on occupied or unconfirmed blocks",
          "status": "defined"
        },
        {
          "id": 2,
          "ucaRef": "UCA-02",
          "constraint": "The Door Controller shall keep the doors closed unless a validated stopped-and-berthed signal is present.",
          "enforcementMechanism": "Door interlock tied to the validated motion and berth status",
          "status": "defined"
        },
        {
          "id": 3,
          "ucaRef": "UCA-03",
          "constraint": "The Train Control Controller shall start braking before the train reaches a lower-speed section so the authorised speed is met at the boundary.",
          "enforcementMechanism": "Speed-supervision curve that anticipates section speed transitions",
          "status": "defined"
        }
      ]
    },
    "step5Information": {
      "lossScenarios": [
        {
          "id": "LS-01",
          "description": "Braking is omitted because degraded train-to-wayside communication delivers stale block-occupancy data, so the process model of the controller wrongly assumes the preceding block is clear and the train continues toward an occupied block.",
          "associatedUnsafeBehaviorIds": ["UCA-01", "HC-01"],
          "sourceRationale": "Derived from UCA-01 and HC-01 (loss of safe separation)."
        },
        {
          "id": "LS-02",
          "description": "Doors are commanded open while the train is still moving because the door controller relies on an out-of-date stopped-and-berthed status from a delayed feedback signal.",
          "associatedUnsafeBehaviorIds": ["UCA-02"],
          "sourceRationale": "Derived from UCA-02 (door operation in motion)."
        },
        {
          "id": "LS-03",
          "description": "Braking is applied too late on entry to a lower-speed section because the speed-limit transition is detected only after the train has already entered the restricted block.",
          "associatedUnsafeBehaviorIds": ["UCA-03"],
          "sourceRationale": "Derived from UCA-03 (overspeed in restricted section)."
        }
      ],
      "safetyRequirements": [
        {
          "id": "SR-01",
          "description": "The CRCS shall command service braking within 200 ms whenever block-occupancy feedback indicates, or fails to confirm within the timeout, that the preceding block is not clear; stale feedback shall be treated as occupied.",
          "addressedLossScenarioIds": ["LS-01"]
        },
        {
          "id": "SR-02",
          "description": "The CRCS shall inhibit any door-open command unless a validated train-stopped-and-berthed signal is present, and shall re-verify that signal within 100 ms of issuing the command.",
          "addressedLossScenarioIds": ["LS-02"]
        },
        {
          "id": "SR-03",
          "description": "The CRCS shall begin braking before a train crosses into a lower-speed section so that the authorised speed is met at the section boundary.",
          "addressedLossScenarioIds": ["LS-03"]
        }
      ]
    },
    "step6LossScenarios": {
      "lossScenarios": [
        {
          "id": 1,
          "uca": "UCA-01",
          "hazard": "H-1",
          "outcome": "The train enters an occupied block, leading to a train-to-train collision.",
          "severity": "Critical",
          "mitigations": ["Treat stale occupancy data as occupied", "Add independent secondary block detection"],
          "status": "open"
        },
        {
          "id": 2,
          "uca": "UCA-02",
          "hazard": "H-2",
          "outcome": "Doors open while moving, leading to a passenger fall at the platform edge.",
          "severity": "Major",
          "mitigations": ["Hard door interlock on validated berth status", "Re-verify berth status after the open command"],
          "status": "open"
        },
        {
          "id": 3,
          "uca": "UCA-03",
          "hazard": "H-3",
          "outcome": "The train overspeeds in a restricted section.",
          "severity": "Major",
          "mitigations": ["Anticipatory speed supervision", "Automatic braking at the section boundary"],
          "status": "mitigated"
        }
      ],
      "safetyRequirements": [
        {
          "id": 1,
          "title": "SR-01 Command braking on an unconfirmed clear block",
          "linkedScenario": 1,
          "category": "functional",
          "owner": "Signalling Safety Team",
          "dueDate": "2026-07-15",
          "status": "planned"
        },
        {
          "id": 2,
          "title": "SR-02 Inhibit door opening unless stopped and berthed",
          "linkedScenario": 2,
          "category": "functional",
          "owner": "Door Systems Team",
          "dueDate": "2026-07-22",
          "status": "planned"
        },
        {
          "id": 3,
          "title": "SR-03 Anticipatory braking for speed transitions",
          "linkedScenario": 3,
          "category": "performance",
          "owner": "Train Control Team",
          "dueDate": "2026-07-29",
          "status": "in-progress"
        }
      ]
    },
    "step7ModelUpdate": {
      "updatedSteps": {
        "step1": true,
        "step2": true,
        "step3": true,
        "step4": true,
        "step5": true
      },
      "modelChanges": [
        {
          "id": 1,
          "area": "Train Control",
          "change": "Added a stale-feedback-means-occupied guard so braking is commanded whenever block occupancy cannot be confirmed.",
          "driver": "UCA-01 and HC-01",
          "impact": "Removes the loss-of-separation scenario LS-01",
          "status": "implemented",
          "evidence": ["SIM-CRCS-101", "TEST-LOG-244"]
        },
        {
          "id": 2,
          "area": "Door Control",
          "change": "Tied the door-open command to a validated stopped-and-berthed interlock with re-verification.",
          "driver": "UCA-02",
          "impact": "Removes the door-in-motion scenario LS-02",
          "status": "implemented",
          "evidence": ["SIM-CRCS-118"]
        }
      ],
      "validationTasks": [
        {
          "id": 1,
          "name": "Hardware-in-the-loop safe-separation test",
          "owner": "Verification and Validation Team",
          "dueDate": "2026-07-20",
          "channel": "rail-test-lab",
          "status": "queued"
        },
        {
          "id": 2,
          "name": "Door interlock regression test",
          "owner": "Door Systems Team",
          "dueDate": "2026-07-27",
          "channel": "rail-test-lab",
          "status": "queued"
        }
      ],
      "integrationNotes": [
        {
          "id": 1,
          "summary": "Signalling and door-control interlocks aligned across the updated control structure.",
          "createdOn": "2026-06-09",
          "author": "Systems Engineer",
          "actionItems": ["Run the full regression suite", "Update the operations playbook"]
        }
      ]
    }
  }'::jsonb,
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP
)
ON CONFLICT (project_id) DO UPDATE
SET document = EXCLUDED.document,
    updated_at = CURRENT_TIMESTAMP;

SELECT setval(
  pg_get_serial_sequence('project', 'id'),
  (SELECT GREATEST(COALESCE(MAX(id), 1), 1) FROM project),
  true
);
