// Static resource/XML audit only. Does not compile or execute application tests.
const fs=require('fs'),path=require('path'),crypto=require('crypto');
const deps='C:/Users/Galyaminsky/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/';
const xml=require(deps+'xml-js'),sharp=require(deps+'sharp');
const base='app/src/main/res/';
const walk=p=>fs.readdirSync(p,{withFileTypes:true}).flatMap(e=>e.isDirectory()?walk(path.join(p,e.name)):[path.join(p,e.name)]);
const nodes=e=>(e.elements||[]).filter(x=>x.type==='element');
const all=e=>[e,...nodes(e).flatMap(all)];
const parse=p=>xml.xml2js(fs.readFileSync(p,'utf8'));
const baseline=JSON.parse(fs.readFileSync('diagnostics/enot_redesign_before.json','utf8'));
const report={xmlFiles:0,errors:[],missingIds:[],changedVariables:[],removedBindings:[],missingReferences:[],oldIllustrations:[],assets:[],changedLayouts:[],newResources:[]};
// Undo only incidental trailing whitespace on layouts untouched by the migration.
for(const name of ['ncs_v2_documents_dialog.xml','ncs_v2_feedback_item.xml','ncs_v2_metric_item.xml','ncs_v2_hub_future_card.xml']){
 const p=base+'layout/'+name;let s=fs.readFileSync(p,'utf8');
 const choices=[s,s.replace(/^\uFEFF/,''),s.replace(/\r?\n$/,''),s.replace(/^\uFEFF/,'').replace(/\r?\n$/,'')];
 for(const c of choices)if(crypto.createHash('sha256').update(c).digest('hex')===baseline[name].sha256){fs.writeFileSync(p,c);break;}
}
const files=walk(base), resources=new Map();
function add(type,name){if(!resources.has(type))resources.set(type,new Set());resources.get(type).add(name);}
for(const f of files){const dir=path.basename(path.dirname(f)).split('-')[0];if(dir!=='values')add(dir,path.basename(f).replace(/\.[^.]+$/,''));}
for(const f of files.filter(f=>f.endsWith('.xml'))){let d;try{d=parse(f);report.xmlFiles++;}catch(e){report.errors.push({file:f,error:e.message});continue;}
 for(const e of all(d)){
  if(path.basename(path.dirname(f)).startsWith('values')&&e.attributes?.name)add(e.name==='item'?e.attributes.type:e.name,e.attributes.name);
  const id=e.attributes?.['android:id'];if(id?.startsWith('@+id/'))add('id',id.slice(5));
 }
}
for(const f of files.filter(f=>f.endsWith('.xml'))){let d;try{d=parse(f);}catch{continue;}
 for(const e of all(d))for(const [k,v]of Object.entries(e.attributes||{})){
  for(const m of v.matchAll(/@(?!android:|\+)([a-z]+)\/([a-zA-Z0-9_.]+)/g))if(!resources.get(m[1])?.has(m[2]))report.missingReferences.push({file:f,key:k,ref:m[0]});
  if(path.dirname(f).endsWith('layout')&&/ncs_v2_/.test(f)&&/@drawable\/(owl_1|brain|girl_2|girl_qes|kids|ic_smile|ic_sad|cuub_1)\b/.test(v))report.oldIllustrations.push({file:f,key:k,value:v});
 }
}
for(const [name,before]of Object.entries(baseline)){
 const p=base+'layout/'+name,d=parse(p),elements=all(d),ids=elements.map(e=>e.attributes?.['android:id']).filter(Boolean);
 for(const id of before.ids)if(!ids.includes(id))report.missingIds.push({file:name,id});
 const variables=elements.filter(e=>e.name==='variable').map(e=>e.attributes);
 if(JSON.stringify(variables)!==JSON.stringify(before.variables))report.changedVariables.push(name);
 const bindings=elements.flatMap(e=>Object.entries(e.attributes||{}).filter(([k,v])=>v.startsWith('@{')).map(([k,v])=>({id:e.attributes?.['android:id'],k,v})));
 for(const b of before.bindings)if(!bindings.some(n=>JSON.stringify(n)===JSON.stringify(b)))report.removedBindings.push({file:name,...b});
 const hash=crypto.createHash('sha256').update(fs.readFileSync(p)).digest('hex');if(hash!==before.sha256)report.changedLayouts.push(name);
}
(async()=>{
 for(const f of files.filter(f=>/drawable-nodpi/.test(f)&&f.endsWith('.png'))){const meta=await sharp(f).metadata();const stats=await sharp(f).stats();const source='figma_assets/'+path.basename(f);report.assets.push({name:path.basename(f),width:meta.width,height:meta.height,hasAlpha:meta.hasAlpha,transparent:meta.hasAlpha&&stats.channels.at(-1).min===0,bytes:fs.statSync(f).size,identicalToExport:fs.existsSync(source)&&fs.readFileSync(source).equals(fs.readFileSync(f))});}
 const used=files.filter(f=>f.endsWith('.xml')).map(f=>fs.readFileSync(f,'utf8')).join('\n')+fs.readFileSync('app/src/main/java/me/proton/jobforandroid/compositionofnumbers/presentation/ComparisonGameFragment.kt','utf8');
 for(const name of ['ncs_v2_brand_header','decor_multiply_divide','ncs_v2_expression_ink','ncs_v2_brand_subtitle_ink'])report.newResources.push({name,used:['layout','drawable','color'].some(type=>used.includes('@'+type+'/'+name))});
 report.unexpectedRemovedBindings=report.removedBindings.filter(b=>!((b.file==='ncs_v2_comparison_result.xml'&&['android:src','android:scaleX'].includes(b.k))||(b.file==='ncs_v2_comparison_rules.xml'&&b.k==='android:visibility')||(['ncs_v2_progress_item.xml','ncs_v2_timer_item.xml','ncs_v2_metric_item.xml','ncs_v2_feedback_item.xml'].includes(b.file)&&b.k==='android:textColor')));
 report.bindingNotes='Only artwork, mirrored legacy artwork, constant VISIBLE and visual colors changed. All data variables, IDs, input values, counters, visibility conditions, selected states, correctness expressions and domain thresholds retained.';
 fs.writeFileSync('diagnostics/enot_static_audit_2026-11-17.json',JSON.stringify(report,null,2));
 console.log(JSON.stringify(report,null,2));
})();
