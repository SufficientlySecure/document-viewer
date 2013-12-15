/* {{Projet:JavaScript/Script|MoveResizeAbsolute}} */

/* <noinclude>

Funzioni standard per la generazione degli elementi in posizione "absolute" : 
* function "move",
* function "resize"

Vedere pagina di discussione.</noinclude>
== CODICE SORGENTE ==
<!--
*/

/* -->
=== FUNCTION: browser ===

* Restituisce true se il browser è Internet Explorer

<!--
*/
// --><div style="border:1px dashed green;margin:1em;padding:1em;"><source lang=javascript>
//<pre><nowiki>

function MoveResizeAbsolute_NavIsIE(){
     var agt=navigator.userAgent.toLowerCase();
     var is_ie = ((agt.indexOf("msie") != -1) && (agt.indexOf("opera") == -1));
     return is_ie;
}

//</nowiki></pre></source></div><!--
/*
-->
=== FUNCTION: larghezza dello schermo ===
* Restituisce la larghezza dello schermo (in pixel)

<!--
*/
// --><div style="border:1px dashed green;margin:1em;padding:1em;"><source lang=javascript>
//<pre><nowiki>

function MoveResizeAbsolute_GetScreenWidth(){
     if(MoveResizeAbsolute_NavIsIE()){
          var ScreenWidth = parseInt(screen.width);
     }else{
          var ScreenWidth = parseInt(window.innerWidth);
     }
     return ScreenWidth;
}

//</nowiki></pre></source></div><!--
/*
-->
=== FUNCTION : altezza dello schermo ===
* Restituisce l'altezza dello schermo (in pixel)

<!--
*/
//--><div style="border:1px dashed green;margin:1em;padding:1em;"><source lang=javascript>
//<pre><nowiki>

function MoveResizeAbsolute_GetScreenHeight(){
     if(MoveResizeAbsolute_NavIsIE()){
          var ScreenHeight = parseInt(screen.height);
     }else{
          var ScreenHeight = parseInt(window.innerHeight);
     }
     return ScreenHeight;
}

//</nowiki></pre></source></div><!--
/*
--> 
=== FUNCTION : MOVE ===

Trasforma un elemento in ancora muoverne un altro (in posizione fixed)
* elementArea = elemento ancora (obbligatorio)
* elementsToMove = elemento da muovere (obbligatorio)
* LeftLimit = limite sinistro, in pixel (facoltativo)
* TopLimit = limite alto, in pixel (facoltativo)

<!--
*/
//--><div style="border:1px dashed green;margin:1em;padding:1em;"><source lang=javascript>
//<pre><nowiki>

function MoveResizeAbsolute_AddMoveArea(elementArea, elementsToMove, LeftLimit, TopLimit){
     if((!elementArea)||(!elementsToMove)) return;
     elementArea.onmousedown=function(event) {
          var monbody = document.body;
          if(!event) { event = window.event; }
          if(MoveResizeAbsolute_NavIsIE()){ 
               positionSouris_X = parseInt( event.clientX + monbody.scrollLeft );
               positionSouris_Y = parseInt( event.clientY + monbody.scrollTop );
          }else{
               positionSouris_X = parseInt( event.pageX );
               positionSouris_Y = parseInt( event.pageY );
          } 
          for(var a=0;a<elementsToMove.length;a++){
               elementsToMove[a].initialX = parseInt( positionSouris_X - elementsToMove[a].offsetLeft);
               elementsToMove[a].initialY = parseInt( positionSouris_Y - elementsToMove[a].offsetTop);
               elementsToMove[a].style.opacity = '.8';
          }
          monbody.onmousemove = function(event) {
               if(!event) { event = window.event; }
               if(MoveResizeAbsolute_NavIsIE()){
                    positionSouris_X = parseInt( event.clientX + monbody.scrollLeft );
                    positionSouris_Y = parseInt( event.clientY + monbody.scrollTop );
               }else{
                    positionSouris_X = parseInt( event.pageX );
                    positionSouris_Y = parseInt( event.pageY );
               }
               var LeftLimitDone = false;
               var TopLimitDone = false;
               for(var a=0;a<elementsToMove.length;a++){
                    PositionGauche = parseInt( positionSouris_X ) - elementsToMove[a].initialX;
                    PositionHaut = parseInt(positionSouris_Y ) - elementsToMove[a].initialY;
                    if(LeftLimit){
                         if(LeftLimit[a]|| LeftLimit[a]==0){
                              if( PositionGauche < parseInt(LeftLimit[a])){
                                   PositionGauche = LeftLimit[a];
                                   LeftLimitDone = true;
                              }
                         }
                    }
                    if(TopLimit){
                         if(TopLimit[a]||TopLimit[a]==0){
                              if( PositionHaut < parseInt(TopLimit[a])){
                                   PositionHaut = parseInt(TopLimit[a]);
                                   TopLimitDone = true;
                              }
                         }
                    }
                    elementsToMove[a].style.left = PositionGauche + 'px';
                    elementsToMove[a].style.top = PositionHaut + 'px';
               }
               if(LeftLimitDone){
                    for(var a=0;a<elementsToMove.length;a++){
                         if(LeftLimit[a]) elementsToMove[a].style.left = LeftLimit[a] + 'px';
                    }
                    LeftLimitDone = false;
               }
               if(TopLimitDone){
                    for(var a=0;a<elementsToMove.length;a++){
                         if(TopLimit[a]) elementsToMove[a].style.top = TopLimit[a] + 'px';
                    }
                    TopLimitDone = false;
               }
          } 
          monbody.onmouseup=function(event) {
               for(var a=0;a<elementsToMove.length;a++){
                    elementsToMove[a].style.opacity = '';
               }
               monbody.onmousemove = null;
               monbody.onmouseup = null;
          }
     }
     elementArea.style.cursor = "move";
}

//</nowiki></pre></source></div><!--
/*
-->
=== FUNCTION : RESIZE ===

Trasforma un elemento in ancora per ridimensionarne un altro (in posizione fixed)
* elementArea = elemento ancora (obbligatorio)
* elementsToResize = elementi da ridimensionare (obbligatorio, in un array)
* MinWidth = larghezza minima, in pixel (facoltativo)
* MinHeight = altezza minima, in pixel (facoltativo)

<!--
*/
//--><div style="border:1px dashed green;margin:1em;padding:1em;"><source lang=javascript>
//<pre><nowiki>

function MoveResizeAbsolute_AddResizeArea(elementArea, elementsToResize, MinWidth, MinHeight){
     if((!elementArea)||(!elementsToResize)) return;
     elementArea.onmousedown = function(event){
          var monbody = document.body;
          if(!event) { event = window.event; }
          if(MoveResizeAbsolute_NavIsIE()){ 
               positionSouris_X = parseInt( event.clientX + monbody.scrollLeft );
               positionSouris_Y = parseInt( event.clientY + monbody.scrollTop );
          }else{
               positionSouris_X = parseInt( event.pageX );
               positionSouris_Y = parseInt( event.pageY );
          }
          for(var a=0;a<elementsToResize.length;a++){
               elementsToResize[a].initialWidth = parseInt( positionSouris_X - elementsToResize[a].offsetWidth );
               elementsToResize[a].initialHeight = parseInt( positionSouris_Y - elementsToResize[a].offsetHeight );
               elementsToResize[a].style.opacity = '.8';
          }
          monbody.onmousemove=function(event) {
               if(!event) { event = window.event; }
               if(MoveResizeAbsolute_NavIsIE()){ 
                    positionSouris_X = parseInt( event.clientX + monbody.scrollLeft );
                    positionSouris_Y = parseInt( event.clientY + monbody.scrollTop );
               }else{
                    positionSouris_X = parseInt( event.pageX );
                    positionSouris_Y = parseInt( event.pageY );
               }
               var MinWidthDone = false;
               var MinHeightDone = false;
               for(var a=0;a<elementsToResize.length;a++){
                    var NewWidth = parseInt( positionSouris_X - elementsToResize[a].initialWidth  );
                    var NewHeight = parseInt( positionSouris_Y - elementsToResize[a].initialHeight );
                    if(MinWidth){
                         if(MinWidth[a] || MinWidth[a]==0){
                              if(NewWidth<parseInt(MinWidth[a])){
                                   NewWidth = MinWidth[a];
                                   MinWidthDone = true;
                              }
                         }
                    }
                    if(MinHeight){
                         if(MinHeight[a] || MinHeight[a]==0){
                              if(NewHeight<parseInt(MinHeight[a])){
                                   NewHeight = MinHeight[a];
                                   MinHeightDone = true;
                              }
                         }
                    }
                    elementsToResize[a].style.width  = NewWidth + 'px';
                    elementsToResize[a].style.height = NewHeight + 'px';
               }
               if(MinWidthDone){
                    for(var a=0;a<elementsToResize.length;a++){
                         if(MinWidth[a]) elementsToResize[a].style.width  = MinWidth[a] + 'px';
                    }
               }
               if(MinHeightDone){
                    for(var a=0;a<elementsToResize.length;a++){
                         if(MinHeight[a]) elementsToResize[a].style.height  = MinHeight[a] + 'px';
                    }
               }

          }
          monbody.onmouseup=function(event) {
               for(var a=0;a<elementsToResize.length;a++){
                    elementsToResize[a].style.opacity = '';
               }
               monbody.onmousemove = null;
               monbody.onmouseup = null;
          }
     }
     elementArea.style.cursor = "se-resize";
}
//</nowiki></pre></source></div>