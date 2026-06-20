import { buildPistarModelFromStepTwoPayload } from './pistar-model.builder';

describe('buildPistarModelFromStepTwoPayload', () => {
  it('materializes a same-actor ObstructLink from a hazard obstructs list and skips cross-actor obstructs', () => {
    const payload = {
      information: {
        modelName: 'CRCS',
        actors: [
          {
            id: 'actor-1',
            name: 'Shuttle',
            type: 'Actor',
            intentionalElements: [
              { id: 'sg-1', name: 'Prevent collisions', type: 'SafetyGoal' },
              { id: 'haz-1', name: 'Misses obstacle', type: 'Hazard', obstructs: ['sg-1', 'sg-2'] }
            ]
          },
          {
            id: 'actor-2',
            name: 'Control Center',
            type: 'Role',
            intentionalElements: [{ id: 'sg-2', name: 'Maintain oversight', type: 'SafetyGoal' }]
          }
        ],
        dependencies: []
      }
    };

    const result = buildPistarModelFromStepTwoPayload(payload);

    expect(result).not.toBeNull();
    const obstructLinks = result!.model.links.filter((link) => link.type === 'istar.ObstructLink');
    expect(obstructLinks.length).toBe(1);
    expect(obstructLinks[0]).toEqual(jasmine.objectContaining({ source: 'haz-1', target: 'sg-1' }));
    // The hazard also lists sg-2, but it lives in another actor, so piStar could not render that
    // obstruct link and the builder must not emit it.
    expect(result!.model.links.some((link) => link.type === 'istar.ObstructLink' && link.target === 'sg-2')).toBeFalse();
  });

  it('does not emit obstruct links when a hazard has no obstructs ids', () => {
    const payload = {
      information: {
        modelName: 'CRCS',
        actors: [
          {
            id: 'actor-1',
            name: 'Shuttle',
            type: 'Actor',
            intentionalElements: [
              { id: 'sg-1', name: 'Prevent collisions', type: 'SafetyGoal' },
              { id: 'haz-1', name: 'Misses obstacle', type: 'Hazard' }
            ]
          }
        ],
        dependencies: []
      }
    };

    const result = buildPistarModelFromStepTwoPayload(payload);

    expect(result).not.toBeNull();
    expect(result!.model.links.some((link) => link.type === 'istar.ObstructLink')).toBeFalse();
  });
});
