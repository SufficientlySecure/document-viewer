// <source lang="JavaScript">

/**
 * Workaround for [[bugzilla:708]] via [[Template:InterProject]].
 * Originally based on code from [[wikt:de:MediaWiki:Common.js]] by [[wikt:de:User:Melancholie]],
 * cleaned up and modified for compatibility with the Vector skin.
 *
 * XXX: The current implementation requires that this code run _before_ jQuery.ready, otherwise
 * the new portal won't have the collapse/uncollapse event handlers attached.  This is fragile
 * and should be fixed somehow; ideally, the Vector extension would expose a function we could
 * call to attach the handlers after the fact, if necessary.
 *
 * Maintainer(s): [[User:Ilmari Karonen]]
 */
$( function () {
    if ( document.getElementById('p-interproject') ) return;  // avoid double inclusion

    mw.util.addCSS( '#interProject,#sisterProjects { display:none }' );

    var interPr = document.getElementById('interProject');
    var sisterPr = document.getElementById('sisterProjects');
    if (!interPr) return;

    var toolBox = document.getElementById('p-tb');
    var panel;
    if (toolBox) {
        panel = toolBox.parentNode;
    } else {
        // stupid incompatible skins...
        var panelIds = ['panel', 'column-one', 'mw_portlets', 'mw-panel'];
        for (var i = 0; !panel && i < panelIds.length; i++) {
             panel = document.getElementById(panelIds[i]);
        }
        // can't find a place for the portlet, try to undo hiding
        if (!panel) {
            mw.util.addCSS( '#interProject,#sisterProjects { display:block }' );
            return;
        }
    }

    var interProject = document.createElement("div");
    interProject.id = "p-interproject";

    interProject.className = ( mw.config.get('skin') == 'vector' ? 'portal' : 'portlet');

    interProject.innerHTML =
        '<h5>' +
        (sisterPr && sisterPr.firstChild ? sisterPr.firstChild.innerHTML : "Sister Projects") +
        '<\/h5><div class="' + ( mw.config.get('skin') == "vector" ? "body" : "pBody") +'">' +
        interPr.innerHTML + '<\/div>';

    if (toolBox && toolBox.nextSibling) {
        panel.insertBefore( interProject, toolBox.nextSibling );
    } else {
        panel.appendChild(interProject);
    }
    var state = $.cookie( 'vector-nav-' + interProject.id );
    if (state == 'true') {
        interProject.className += " expanded";
        $(interProject).find('.body').show();
    } else {
        interProject.className += ' collapsed';
    }
} );