var WikiaScriptLoader={};(function(){var userAgent=navigator.userAgent.toLowerCase(),useDOMInjection=(userAgent.indexOf('opera')!=-1)||(userAgent.indexOf('firefox/3.')!=-1),headNode=document.getElementsByTagName('HEAD')[0],NodeFactories={js:function(url,options){var script=document.createElement('script');script.src=url;script.type="text/javascript";script.async=false;script.onLoadDone=false;script.onLoadCallback=options.callback||null;script.onload=function(){if(!this.onLoadDone&&typeof this.onLoadCallback=='function'){this.onLoadCallback();this.onLoadDone=true;}};script.onreadystatechange=function(){if(!this.onloadDone&&this.readyState=='loaded'&&typeof this.onLoadCallback=='function'){this.onLoadCallback();this.onLoadDone=true;}};return script;},css:function(url,options){var link=document.createElement('link');link.href=url;link.rel='stylesheet';link.type='text/css';link.media=options.media||'';return link;}},counter=0;function isArray(obj){return obj instanceof Array;}
function injectNode(type,urls,options){options=options||{};if(!isArray(urls)){urls=[urls];}
var node,url,finalCallback,opts;if(options.callback){finalCallback=function(){counter--;if(counter==0){options.callback();}}}
if(type=='js'){opts={callback:finalCallback};counter+=urls.length;}else{opts=options;}
for(var x=0,y=urls.length;x<y;x++){headNode.appendChild(NodeFactories[type](urls[x],opts));}}
function buildScript(urls){var output='';if(!isArray(urls))
urls=[urls];for(var x=0,y=urls.length;x<y;x++){if(typeof urls[x]==='string'){output+='<scr'+'ipt src="'+urls[x]+'" type="text/javascript"></scr'+'ipt>';}}
return output;}
function writeScript(urls,callback){var output=buildScript(urls);document.write(output);if(typeof callback=='function'){var handler=function(){callback();};if(window.addEventListener)
window.addEventListener('load',handler,false);else if(window.attachEvent)
window.attachEvent('onload',handler);}}
this.buildScript=buildScript;this.loadScript=function(urls,callback){if(useDOMInjection)
injectNode('js',urls,{callback:callback});else
writeScript(urls,callback);};this.loadCSS=function(urls,media){injectNode('css',urls,{media:media});};}).apply(WikiaScriptLoader);window.wsl=WikiaScriptLoader;;(function(window){var jQueryRequested=false;window.getJqueryUrl=function(){var result=[];if(typeof window.jQuery=='undefined'&&jQueryRequested==false){jQueryRequested=true;result=result.concat(window.wgJqueryUrl);}
return result;};})(window);;


(function(c){var treatments=c.abTreatments||{},tracked={},reg=new RegExp('[\\?&]+ab\\[([^&]+)\\]=([^&]*)','gi'),matches;while((matches=reg.exec(c.location.href))!=null){treatments[matches[1]]=matches[2];tracked[matches[1]]=true;}
c.abTreatments=treatments;c.abBeingTracked=tracked;c.getTreatmentGroup=function(expId){var hasLogging=!!c.console,treatmentGroup="";if(typeof abTreatments[expId]!=='undefined'){treatmentGroup=abTreatments[expId];}else{if(AB_CONFIG.hasOwnProperty(expId)){var tgId,tgConfig;if(true||typeof c.beacon_id=="undefined"){if(hasLogging){if(!c.wgJsAtBottom){console.log("This type of page makes a beacon call in body but loads JS for tests in the head: A/B tests can't work this way. Will fall back to control group.");}else{console.log("DON'T CALL getTreatmentGroup() BEFORE BEACON/PAGE-VIEW CALL! Experiment is broken (will fall back to control group).");}}
for(tgId in AB_CONFIG[expId]['groups']){tgConfig=AB_CONFIG[expId]['groups'][tgId];if(tgConfig.is_control){treatmentGroup=tgId;}}
if((treatmentGroup==="")&&hasLogging){console.log("NO CONTROL GROUP DEFINED FOR EXPERIMENT: "+expId);}
abBeingTracked[expId]=false;}else{var normalizedHash=c.abHash(expId),controlId;for(tgId in AB_CONFIG[expId]['groups']){tgConfig=AB_CONFIG[expId]['groups'][tgId];if((normalizedHash>=tgConfig.min)&&(normalizedHash<=tgConfig.max)){treatmentGroup=tgId;}
if(tgConfig.is_control){controlId=tgId;}}
if(treatmentGroup===""){treatmentGroup=controlId;abBeingTracked[expId]=false;}else{WikiaTracker.trackEvent('ab_treatment',{'varnishTime':varnishTime,'experimentId':expId,'treatmentGroup':treatmentGroup},'internal');abTreatments[expId]=treatmentGroup;abBeingTracked[expId]=true;}}}else if(hasLogging){console.log("CALLED getTreatmentGroup FOR AN EXPERIMENT THAT IS NOT CONFIGURED! Exp: "+expId);}};return treatmentGroup;};c.abHash=function(expId){if(typeof c.beacon_id=="undefined"){return 0;}else{var s=c.beacon_id+''+expId,hash=0,i;for(i=0;i<s.length;i++){hash+=(s.charCodeAt(i)*(i+1));}
return Math.abs(hash)%100;}};})(window);;