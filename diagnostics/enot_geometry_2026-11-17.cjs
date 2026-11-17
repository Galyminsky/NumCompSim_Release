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
{
 const f='layout/ncs_v2_main_hub.xml',d=read(f);
 for(const id of ['ncs_v2_hub_arithmetic_card','ncs_v2_hub_comparison_card']){
  const card=all(d).find(e=>get(e,'id')==='@+id/'+id);set(card,{paddingTop:'3dp',minHeight:'182dp'});
  const title=nodes(card).find(e=>e.name==='TextView');set(title,{layout_marginTop:null});
 }
 write(f,d);
}
{
 const f='layout/ncs_v2_welcome.xml',d=read(f),c=nodes(visual(d))[0],bottom=nodes(c).at(-1);
 const docs=nodes(bottom).find(e=>get(e,'id')==='@+id/ncs_v2_documents_button'),caption=nodes(bottom).find(e=>get(e,'text')==='@string/ncs_v2_small_discovery');
 bottom.elements=nodes(bottom).filter(e=>e!==docs&&e!==caption);
 set(caption,{layout_width:'0dp',layout_weight:'1'});set(docs,{layout_marginTop:null});
 bottom.elements.unshift({type:'element',name:'LinearLayout',attributes:{'android:layout_width':'match_parent','android:layout_height':'wrap_content','android:orientation':'horizontal','android:gravity':'center_vertical'},elements:[caption,docs]});
 write(f,d);
}
// Avoid overlap between growing progress labels and the native progress track.
{
 const f='layout/ncs_v2_progress_item.xml',d=read(f),r=visual(d);set(r,{layout_height:'wrap_content',minHeight:'87dp'});set(nodes(r)[0],{paddingBottom:'28dp'});write(f,d);
 const f2='layout/ncs_v2_timer_item.xml',d2=read(f2);set(visual(d2),{layout_height:'wrap_content',minHeight:'@dimen/ncs_v2_timer_height'});write(f2,d2);
 const f3='layout/ncs_v2_game.xml',d3=read(f3);
 for(const e of all(d3)){
  if(get(e,'layout_height')==='87dp')set(e,{layout_height:'wrap_content',minHeight:'87dp'});
  if(get(e,'id')==='@+id/ncs_v2_game_timer')set(e,{layout_height:'wrap_content'});
 }
 write(f3,d3);
}
// Keep unanswered cards in their default visual surface during the feedback lock.
{
 const f='drawable/ncs_v2_answer_card.xml',d=read(f);for(const e of all(d))if(e.name==='solid'&&get(e,'color')==='@color/ncs_v2_disabled')set(e,{color:'@color/ncs_v2_paper'});write(f,d);
}
console.log('Geometry checks applied.');

