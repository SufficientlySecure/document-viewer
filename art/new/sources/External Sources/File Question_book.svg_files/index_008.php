$j(document).ready(function() {
    importStylesheet('MediaWiki:CollapsibleTemplates.css');

    var slideDuration = (skin == 'vector') ? 150 : 0;

    $j('div.collapsibleheader').show();

    $j('div.collapsibletemplate.collapsed div.body').hide();
    
    $j('table.collapsible.collapsed > tbody > tr:not(:first-child)').toggleClass('hidden');

    $j('div.collapsibletemplate div.body').removeClass('show-on-commons');

    function toggleTemplate($element) {
        if ($element.is('tr')) {
            $element
            .parent().parent()
            .toggleClass('collapsed');

            $element.nextAll('tr')
            .toggleClass('hidden');
        } else {
            $element
            .parent()
            .toggleClass('expanded')
            .toggleClass('collapsed')
            .find('div.body')
            .slideToggle(slideDuration);
        }
    }
    var $headings = $j('div.collapsibletemplate > div.collapsibleheader, table.collapsible > tbody >tr:first-child');
    $headings.mousedown(function(e) {
        if ($j(e.target).is('a')) {
          return true;
        } else {
          toggleTemplate($j(this));
          return false;
        }
    });
});