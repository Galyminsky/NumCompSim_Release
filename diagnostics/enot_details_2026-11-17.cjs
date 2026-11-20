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
 const f='layout/ncs_v2_equation_item.xml',d=read(f);
 for(const e of all(d))if(e.attributes?.style==='@style/TextAppearance.NcsV2.Equation'&&get(e,'layout_width')==='@dimen/ncs_v2_equation_slot_width')set(e,{maxLines:'1',autoSizeTextType:'uniform',autoSizeMinTextSize:'24sp',autoSizeMaxTextSize:'49sp',autoSizeStepGranularity:'1sp'});
 write(f,d);
 const dims=read('values/dimens_v2.xml');const gap=all(dims).find(e=>e.attributes?.name==='ncs_v2_equation_gap');gap.elements=[{type:'text',text:'8dp'}];write('values/dimens_v2.xml',dims);
 fs.mkdirSync(base+'values-w360dp',{recursive:true});fs.writeFileSync(base+'values-w360dp/dimens_v2.xml','<?xml version="1.0" encoding="utf-8"?>\n<resources>\n    <!-- Retain the reference gap when the available phone width permits it. -->\n    <dimen name="ncs_v2_equation_gap">13dp</dimen>\n</resources>\n');
}
{
 const f='layout/ncs_v2_feedback_item.xml',d=read(f);set(visual(d),{layout_height:'wrap_content',minHeight:'@dimen/ncs_v2_feedback_height'});
 for(const e of all(d))if(get(e,'textColor')?.includes('correct ? @color/ncs_v2_success'))set(e,{textColor:get(e,'textColor').replace('correct ? @color/ncs_v2_success','correct ? @color/ncs_v2_muted')});
 write(f,d);
 const f2='layout/ncs_v2_game.xml',d2=read(f2);for(const e of all(d2)){if(get(e,'layout_height')==='68dp')set(e,{layout_height:'wrap_content',minHeight:'68dp'});if(get(e,'id')==='@+id/ncs_v2_game_feedback')set(e,{layout_height:'wrap_content'});}write(f2,d2);
 const f3='layout/ncs_v2_metric_item.xml',d3=read(f3);for(const e of all(d3)){
  if(get(e,'id')==='@+id/ncs_v2_metric_target')set(e,{textColor:'@color/ncs_v2_muted'});
  if(get(e,'id')==='@+id/ncs_v2_metric_symbol')set(e,{textColor:'@{achieved ? @color/ncs_v2_muted : @color/ncs_v2_warm_ink}'});
 }write(f3,d3);
}
{
 const f='values/colors_v2.xml',d=read(f),r=nodes(d)[0];
 for(const[name,color]of Object.entries({ncs_v2_expression_ink:'#292647',ncs_v2_brand_subtitle_ink:'#716D88'}))r.elements.push({type:'element',name:'color',attributes:{name},elements:[{type:'text',text:color}]});write(f,d);
 const f2='layout/ncs_v2_comparison_game.xml',d2=read(f2);for(const e of all(d2))if(['@+id/ncs_v2_comparison_expression_left','@+id/ncs_v2_comparison_expression_right'].includes(get(e,'id')))set(e,{textColor:'@color/ncs_v2_expression_ink'});write(f2,d2);
 const f3='layout/ncs_v2_brand_header.xml',d3=read(f3);for(const e of all(d3))if(get(e,'text')==='@string/ncs_v2_schetenot_subtitle')set(e,{textColor:'@color/ncs_v2_brand_subtitle_ink'});write(f3,d3);
 const f4='layout/ncs_v2_comparison_rules.xml',d4=read(f4);for(const e of all(d4)){if(get(e,'text')==='@string/ncs_v2_comparison_rules_example_sign')set(e,{textColor:'@color/ncs_v2_operation_add'});if(['@string/ncs_v2_comparison_rules_example_left','@string/ncs_v2_comparison_rules_example_right'].includes(get(e,'text')))set(e,{textColor:'@color/ncs_v2_expression_ink'});}write(f4,d4);
}
console.log('Confirmed detail colors and small-width arithmetic sizing applied.');

