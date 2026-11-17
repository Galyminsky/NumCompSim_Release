// Historical migration record. Intentionally disabled after successful application.
throw new Error('Migration already applied; use the read-only static audit instead.');
const fs=require('fs');
const xml=require('C:/Users/Galyaminsky/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/xml-js');
const base='app/src/main/res/';
const read=f=>xml.xml2js(fs.readFileSync(base+f,'utf8'),{ignoreComment:true});
const write=(f,d)=>fs.writeFileSync(base+f,xml.js2xml(d,{spaces:4,attributeValueFn:v=>v.replace(/&(?!(?:amp|quot|apos|lt|gt);)/g,'&amp;').replace(/</g,'&lt;')})+'\n');
const children=e=>(e.elements||[]).filter(x=>x.type==='element');
const all=e=>[e,...children(e).flatMap(all)];
const get=(e,k)=>e.attributes?.['android:'+k];
const set=(e,a)=>{e.attributes||={};for(const[k,v]of Object.entries(a)){const key=k.includes(':')||k==='layout'||k==='style'?k:'android:'+k;if(v===null)delete e.attributes[key];else e.attributes[key]=v;}return e;};
const el=(name,a={},elements=[])=>set({type:'element',name,elements},a);
const root=d=>children(d)[0];
const visual=d=>children(root(d)).find(x=>x.name!=='data');
const pic=(name,w,h,a={})=>el('ImageView',{layout_width:w,layout_height:h,contentDescription:'@null',importantForAccessibility:'no',src:'@drawable/'+name,scaleType:'fitCenter',...a});
function header(){return el('include',{layout:'@layout/ncs_v2_brand_header',layout_width:'match_parent',layout_height:'wrap_content'});}
function titleDecor(parent,index){const title=parent.elements[index];set(title,{layout_width:'0dp',layout_weight:'1'});parent.elements[index]=el('LinearLayout',{layout_width:'match_parent',layout_height:'wrap_content',gravity:'center_vertical',orientation:'horizontal',layout_marginTop:get(title,'layout_marginTop')||'0dp'},[title,pic('decor_sparkles','44dp','44dp')]);set(title,{layout_marginTop:null});}
// Use the same brand proportions on both entry screens. Preserve the documents control.
for(const name of []){
 const d=read('layout/'+name+'.xml'), c=children(visual(d))[0];
 if(name.endsWith('main_hub'))c.elements[0]=header();
 else{
  const frame=children(c)[0], docs=all(frame).find(e=>get(e,'id')==='@+id/ncs_v2_documents_button');
  frame.elements=[header()];set(frame,{layout_height:'wrap_content'});
  // Keep legal/document access outside the brand artwork and primary action.
  const bottom=children(c).at(-1);
  set(docs,{layout_gravity:'end',layout_marginTop:'8dp'});bottom.elements.push(docs);
 }
 write('layout/'+name+'.xml',d);
}
// Correct surfaces and selection without changing the four operation palettes.
{
 const d=read('drawable/ncs_v2_surface_outline.xml');for(const e of all(d)){if(e.name==='solid')set(e,{color:'@color/ncs_v2_paper'});if(e.name==='corners')set(e,{radius:'@dimen/ncs_v2_radius_panel'});}write('drawable/ncs_v2_surface_outline.xml',d);
 const s=read('drawable/ncs_v2_selected_outline.xml');for(const e of all(s))if(e.name==='corners')set(e,{radius:'@dimen/ncs_v2_radius_panel'});write('drawable/ncs_v2_selected_outline.xml',s);
 const d2=read('layout/ncs_v2_lesson_item.xml');set(visual(d2),{background:'@{expanded ? @drawable/ncs_v2_lesson_card : @drawable/ncs_v2_panel}'});write('layout/ncs_v2_lesson_item.xml',d2);
 const d3=read('values/styles_v2.xml');const style=all(d3).find(e=>e.attributes?.name==='TextAppearance.NcsV2.Question');style.attributes.parent='TextAppearance.NcsV2.ExtraBold';write('values/styles_v2.xml',d3);
}
// Add the distinct mathematical artwork used by multiplication/division introductions.
fs.copyFileSync('figma_assets/decor_multiply_divide.png',base+'drawable-nodpi/decor_multiply_divide.png');
for(const op of ['multiplication','division']){
 const f='layout/ncs_v2_'+op+'_intro.xml',d=read(f);
 for(const e of all(d))if(get(e,'src')==='@drawable/decor_plus_minus')set(e,{src:'@drawable/decor_multiply_divide'});
 write(f,d);
}
// Preserve aspect ratios, add separate screen decoration, let scaled text grow.
for(const name of ['ncs_v2_lesson_selection','ncs_v2_level_selection','ncs_v2_game','ncs_v2_comparison_game','ncs_v2_result_success','ncs_v2_result_failure','ncs_v2_comparison_result','ncs_v2_addition_intro','ncs_v2_subtraction_intro','ncs_v2_multiplication_intro','ncs_v2_division_intro','ncs_v2_comparison_rules','ncs_v2_comparison_level']){
 const f='layout/'+name+'.xml',d=read(f);
 for(const e of all(d)){
  if(get(e,'lineHeight')){set(e,{lineHeight:null,lineSpacingMultiplier:'1.08'});}
  if(e.name==='ImageView'&&get(e,'src')?.startsWith('@drawable/'))set(e,{importantForAccessibility:'no'});
  if(get(e,'id')?.endsWith('_back_button')||get(e,'id')?.endsWith('_start_button'))set(e,{clickable:'true',focusable:'true',minHeight:'48dp'});
 }
 const c=children(visual(d))[0];
 if(name==='ncs_v2_comparison_game'){
  const i=c.elements.findIndex(e=>get(e,'id')==='@+id/ncs_v2_comparison_game_title');titleDecor(c,i);
 }
 if(name==='ncs_v2_game'||name==='ncs_v2_lesson_selection'){
  const group=children(c).find(e=>children(e).some(x=>x.attributes?.style==='@style/TextAppearance.NcsV2.Question'||x.attributes?.style==='@style/TextAppearance.NcsV2.ScreenTitle'));
  if(group){const i=group.elements.findIndex(x=>x.attributes?.style?.startsWith('@style/TextAppearance.NcsV2.'));titleDecor(group,i);}
 }
 if(name.startsWith('ncs_v2_result_')){
  const i=c.elements.findIndex(e=>get(e,'src')?.match(/raccoon_(success|failure)/));
  if(i>=0){const hero=c.elements[i];c.elements[i]=el('FrameLayout',{layout_width:'match_parent',layout_height:'146dp'},[set(hero,{layout_gravity:'center'}),pic('decor_sparkles','44dp','44dp',{layout_gravity:'end|center_vertical'})]);}
 }
 if(name==='ncs_v2_comparison_result'){
  const group=children(c)[1],hero=children(group)[0];group.elements[0]=el('FrameLayout',{layout_width:'match_parent',layout_height:'146dp'},[set(hero,{layout_gravity:'center'}),pic('decor_sparkles','44dp','44dp',{layout_gravity:'end|center_vertical'})]);
 }
 write(f,d);
}
// Match the grey, blue outlined selected sign in the approved comparison feedback.
write('drawable/ncs_v2_comparison_answer_card.xml',{declaration:{attributes:{version:'1.0',encoding:'utf-8'}},elements:[el('shape',{'xmlns:android':'http://schemas.android.com/apk/res/android',shape:'rectangle'},[el('solid',{color:'@color/ncs_v2_muted'}),el('stroke',{width:'@dimen/ncs_v2_stroke_regular',color:'@color/ncs_v2_primary'}),el('corners',{radius:'@dimen/ncs_v2_radius_panel'})])]});
// Correct the restored welcome copy in every existing language.
for(const [dir,copy] of Object.entries({values:'Играй с числами и узнавай новое вместе со СчётЕнотом.','values-en':'Play with numbers and discover new things with СчётЕнот.','values-uk':'Грай із числами та дізнавайся нове разом зі СчётЕнотом.','values-kk':'СчётЕнотпен бірге сандармен ойнап, жаңа нәрселерді үйрен.'})){
 const f=dir+'/'+(dir==='values'?'strings_v2.xml':'strings.xml'), d=read(f);const s=all(d).find(e=>e.attributes?.name==='ncs_v2_welcome_math_description');s.elements=[{type:'text',text:copy}];write(f,d);
}
console.log('Composition refinement finished.');


