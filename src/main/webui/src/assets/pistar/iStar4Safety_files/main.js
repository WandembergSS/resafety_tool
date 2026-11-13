/*!
 * Embedded goal modeling editor.
 */

$(document).ready(function () {
    'use strict';

        $(document).on('click', 'a[href="#"]', function (event) {
            event.preventDefault();
        });

        istar.graph = istar.setup.setupModel();
        istar.paper = istar.setup.setupDiagram(istar.graph);
        istar.setupMetamodel(istar.metamodel);
        ui.setupUi();

        //wait the ui finish loading before loading a model
        $(document).ready(function () {
            setTimeout(function () {
                if (typeof window.__pistarExternalModelText === 'string' && window.__pistarExternalModelText.trim()) {
                    istar.fileManager.loadModel(window.__pistarExternalModelText);
                }
                ui.selectPaper();//clear selection
            }, 5);
        });
    });

/*definition of globals to prevent undue JSHint warnings*/
/*globals istar:false, ui:false, console:false, $:false */