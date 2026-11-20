const fs=require('fs'),x=require('C:/Users/Galyaminsky/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/xml-js');
const base='app/src/main/res/',kids=e=>(e.elements||[]).filter(e=>e.type==='element'),all=e=>[e,...kids(e).flatMap(all)];
const get=(e,k)=>e.attributes?.['android:'+k];
const read=f=>x.xml2js(fs.readFileSync(base+f,'utf8'));
const before=JSON.parse(fs.readFileSync('diagnostics/enot_phone_polish_before.json'));
const report={errors:[],missingIDs:[],variablesChanged:[],missingRefs:[],changedBindings:[],scrollFallbacks:[]};
const walk=p=>fs.readdirSync(p,{withFileTypes:true}).flatMap(e=>e.isDirectory()?walk(p+'/'+e.name):[p+'/'+e.name]);
const files=walk(base),defs=new Set();
for(const f of files){const type=f.slice(base.length).replace(/^\/+/, '').split('/')[0].split('-')[0];if(type!=='values')defs.add(type+'/'+f.split('/').at(-1).replace(/\.[^.]+$/,''));if(f.endsWith('.xml'))try{const d=x.xml2js(fs.readFileSync(f,'utf8'));for(const e of all(d)){if(type==='values'&&e.attributes?.name)defs.add((e.name==='item'?e.attributes.type:e.name)+'/'+e.attributes.name);const id=get(e,'id');if(id?.startsWith('@+id/'))defs.add('id/'+id.slice(5));}}catch(err){report.errors.push({file:f,error:err.message});}}
for(const f of files.filter(f=>f.endsWith('.xml'))){let d;try{d=x.xml2js(fs.readFileSync(f,'utf8'));}catch{continue;}for(const e of all(d))for(const v of Object.values(e.attributes||{}))for(const m of v.matchAll(/@(?!android:|\+)([a-z]+\/[a-zA-Z0-9_.]+)/g))if(!defs.has(m[1]))report.missingRefs.push({file:f,ref:m[1]});}
for(const[f,b]of Object.entries(before)){const d=read('layout/'+f),a=all(d),ids=a.map(e=>get(e,'id'));for(const id of b.ids)if(!ids.includes(id))report.missingIDs.push({f,id});if(JSON.stringify(a.filter(e=>e.name==='variable').map(e=>e.attributes))!==JSON.stringify(b.variables))report.variablesChanged.push(f);for(const old of b.bindings)if(!a.some(e=>e.attributes?.[old.k]===old.v))report.changedBindings.push({f,...old});if(a.some(e=>e.name==='ScrollView'))report.scrollFallbacks.push(f);}
fs.writeFileSync('diagnostics/enot_phone_polish_audit.json',JSON.stringify(report,null,2));console.log(JSON.stringify(report,null,2));

