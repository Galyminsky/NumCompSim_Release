// Historical migration record. Intentionally disabled after successful application.
throw new Error('Migration already applied; use the read-only static audit instead.');
const fs=require('fs'),xml=require('C:/Users/Galyaminsky/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/xml-js');
const base='app/src/main/res/';
const read=f=>xml.xml2js(fs.readFileSync(base+f,'utf8'));
const write=(f,d)=>fs.writeFileSync(base+f,xml.js2xml(d,{spaces:4,indentAttributes:true,attributeValueFn:v=>v.replace(/&(?!(?:amp|quot|apos|lt|gt);)/g,'&amp;').replace(/</g,'&lt;')})+'\n');
const nodes=e=>(e.elements||[]).filter(x=>x.type==='element');
const all=e=>[e,...nodes(e).flatMap(all)];
const get=(e,k)=>e.attributes?.['android:'+k];
const set=(e,a)=>{for(const[k,v]of Object.entries(a)){if(v===null)delete e.attributes['android:'+k];else e.attributes['android:'+k]=v;}return e;};
const visual=d=>nodes(nodes(d)[0]).find(e=>e.name!=='data');
// Radius and single-expanded-card geometry.
{
 const d=read('drawable/ncs_v2_lesson_card.xml');for(const e of all(d))if(e.name==='corners')set(e,{radius:'@dimen/ncs_v2_radius_panel'});write('drawable/ncs_v2_lesson_card.xml',d);
 const l=read('layout/ncs_v2_lesson_item.xml'),r=visual(l);set(r,{paddingTop:null,paddingBottom:null});
 const row=nodes(r).find(e=>e.name==='LinearLayout');set(row,{paddingTop:'12dp',paddingBottom:'12dp'});
 const badge=all(l).find(e=>get(e,'text')?.includes('ncs_v2_can_play'));set(badge,{background:'@drawable/ncs_v2_indicator_neutral',paddingStart:'12dp',paddingEnd:'12dp'});write('layout/ncs_v2_lesson_item.xml',l);
}
// Hub subtitle spans the content width rather than being squeezed beside the mascot.
{
 const f='layout/ncs_v2_main_hub.xml',d=read(f),c=nodes(visual(d))[0],heading=nodes(c)[1],titleColumn=nodes(heading)[0];
 const subtitle=nodes(titleColumn).find(e=>get(e,'text')==='@string/ncs_v2_hub_subtitle');
 titleColumn.elements=titleColumn.elements.filter(e=>e!==subtitle);
 const i=c.elements.indexOf(heading);c.elements.splice(i+1,0,subtitle);
 write(f,d);
}
// Figma has no extra sparkle beside the large welcome illustration.
{
 const f='layout/ncs_v2_welcome.xml',d=read(f);
 for(const e of all(d))if(e.name==='FrameLayout'&&nodes(e).some(x=>get(x,'src')==='@drawable/raccoon_welcome'))e.elements=nodes(e).filter(x=>get(x,'src')!=='@drawable/decor_sparkles');
 write(f,d);
}
// Figma's typography is Nunito Sans; keep the owner's deferred Nunito family.
// Use relative line height for explanatory text so accessibility scaling can grow.
for(const name of ['ncs_v2_addition_intro','ncs_v2_subtraction_intro','ncs_v2_multiplication_intro','ncs_v2_division_intro']){
 const f='layout/'+name+'.xml',d=read(f);
 for(const e of all(d))if(e.name==='TextView'&&['22dp','24dp'].includes(get(e,'layout_height')))set(e,{layout_height:'wrap_content',minHeight:get(e,'layout_height')});
 write(f,d);
}
// Do not leave the legacy internal project name in screen navigation context.
{
 const f='values/strings_v2.xml',d=read(f);for(const e of all(d))if(e.attributes?.name==='ncs_v2_brand')e.elements=[{type:'text',text:'СчётЕнот'}];write(f,d);
}
// One-line encouragement in the arithmetic rules, as approved in Figma.
for(const dir of ['values','values-en','values-uk','values-kk']){
 const f=dir+'/'+(dir==='values'?'strings_v2.xml':'strings.xml'),d=read(f);
 for(const e of all(d))if(e.attributes?.name==='ncs_v2_together')for(const t of e.elements||[])if(t.type==='text')t.text=t.text.replace(/\\n/g,' ');
 write(f,d);
}
// Reviewable XML: one attribute per line for all modified layouts/drawables.
const audit=JSON.parse(fs.readFileSync('diagnostics/enot_static_audit_2026-11-17.json','utf8'));
for(const f of audit.changedLayouts.concat('ncs_v2_brand_header.xml'))write('layout/'+f,read('layout/'+f));
console.log('Final XML formatting and geometry corrections finished.');

