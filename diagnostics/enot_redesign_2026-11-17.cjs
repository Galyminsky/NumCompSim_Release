// Historical migration record. Intentionally disabled after successful application.
throw new Error('Migration already applied; use the read-only static audit instead.');
// One-off XML migration; no build, tests, dependency or Git operations.
const fs = require('fs');
const path = require('path');
const xml = require('C:/Users/Galyaminsky/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/xml-js');
const crypto = require('crypto');
const base = 'app/src/main/res/';
const read = f => xml.xml2js(fs.readFileSync(base+f,'utf8'), {ignoreComment:true});
const write = (f,d) => fs.writeFileSync(base+f, xml.js2xml(d,{spaces:4})+'\n');
const nodes = e => (e.elements||[]).filter(x=>x.type==='element');
const all = e => [e,...nodes(e).flatMap(all)];
const get = (e,k) => e.attributes?.['android:'+k];
const set = (e,a) => { e.attributes ||= {}; for(const [k,v] of Object.entries(a)) {if(v===null) delete e.attributes[(k.includes(':')||k==='layout'||k==='style')?k:'android:'+k]; else e.attributes[(k.includes(':')||k==='layout'||k==='style')?k:'android:'+k]=v;} return e; };
const el = (name,a={},children=[]) => set({type:'element',name,elements:children},a);
const box = (children,a={}) => el('LinearLayout',{layout_width:'match_parent',layout_height:'wrap_content',orientation:'vertical',...a},children);
const pic = (src,w,h,a={}) => el('ImageView',{layout_width:w,layout_height:h,src:'@drawable/'+src,scaleType:'fitCenter',contentDescription:'@null',importantForAccessibility:'no',...a});
const text = (s,size='16sp',a={}) => el('TextView',{layout_width:'match_parent',layout_height:'wrap_content',fontFamily:'@font/ncs_v2_nunito_bold',includeFontPadding:'false',text:'@string/'+s,textColor:'@color/ncs_v2_primary',textSize:size,...a});
const spacer = h => el('Space',{layout_width:'1dp',layout_height:h});
const root = d => nodes(d)[0];
const visual = d => nodes(root(d)).find(x=>x.name!=='data');
const findId = (d,id) => all(d).find(x=>get(x,'id')==='@+id/'+id);
const byText = (d,t) => all(d).find(x=>get(x,'text')==='@string/'+t);
const includeHeader = () => el('include',{'layout':'@layout/ncs_v2_brand_header',layout_width:'match_parent',layout_height:'wrap_content'});
const scroll = children => el('ScrollView',{layout_width:'match_parent',layout_height:'match_parent',background:'@color/ncs_v2_paper',fitsSystemWindows:'true',fillViewport:'true',overScrollMode:'never'},[box(children,{padding:'@dimen/ncs_v2_space_24',clipChildren:'false',clipToPadding:'false'})]);
function reflow(d,children){ const r=root(d); r.elements=[...nodes(r).filter(x=>x.name==='data'),scroll(children)]; }
function clearPosition(e){for(const k of Object.keys(e.attributes||{})) if(k.startsWith('app:layout_constraint')||/^android:layout_margin(Start|End|Bottom)$/.test(k)) delete e.attributes[k];return e;}
const layoutFiles=fs.readdirSync(base+'layout').filter(x=>x.startsWith('ncs_v2_')&&x.endsWith('.xml'));
const before={};
for(const f of layoutFiles){const raw=fs.readFileSync(base+'layout/'+f,'utf8');const d=read('layout/'+f);before[f]={sha256:crypto.createHash('sha256').update(raw).digest('hex'),ids:all(d).map(x=>get(x,'id')).filter(Boolean),variables:all(d).filter(x=>x.name==='variable').map(x=>x.attributes),bindings:all(d).flatMap(x=>Object.entries(x.attributes||{}).filter(([k,v])=>v.startsWith('@{')).map(([k,v])=>({id:get(x,'id'),k,v})))};}
fs.writeFileSync('diagnostics/enot_redesign_before.json',JSON.stringify(before,null,2));
// Shared brand: the original logo, two brand segments and separate sparkles.
write('layout/ncs_v2_brand_header.xml',{declaration:{attributes:{version:'1.0',encoding:'utf-8'}},elements:[el('FrameLayout',{'xmlns:android':'http://schemas.android.com/apk/res/android',layout_width:'match_parent',layout_height:'wrap_content',minHeight:'72dp',clipChildren:'false'},[
 pic('decor_sparkles','88dp','88dp',{layout_gravity:'end|top',layout_marginTop:'-16dp'}),
 box([pic('logo_schetenot','72dp','72dp'),box([
 box([text('ncs_v2_schetenot_first','27sp',{layout_width:'wrap_content',fontFamily:'@font/ncs_v2_nunito_extrabold'}),text('ncs_v2_schetenot_second','27sp',{layout_width:'wrap_content',fontFamily:'@font/ncs_v2_nunito_extrabold',textColor:'@color/ncs_v2_orange'})],{orientation:'horizontal'}),
 text('ncs_v2_schetenot_subtitle','11sp',{textColor:'@color/ncs_v2_muted'})
 ],{layout_width:'0dp',layout_weight:'1',layout_marginStart:'12dp'})],{orientation:'horizontal',gravity:'center_vertical'})
 ])]});
// Existing scroll screens: retain their data and interaction tree.
for(const f of layoutFiles){if(!['ncs_v2_welcome.xml','ncs_v2_lesson_selection.xml','ncs_v2_level_selection.xml','ncs_v2_game.xml','ncs_v2_result_success.xml','ncs_v2_result_failure.xml',...['addition','subtraction','multiplication','division'].map(o=>'ncs_v2_'+o+'_intro.xml')].includes(f))continue;
 const d=read('layout/'+f);
 for(const e of all(d)){
  if(get(e,'background')==='@drawable/ncs_v2_notebook_background')set(e,{background:'@color/ncs_v2_paper'});
  if(get(e,'paddingTop')==='@dimen/ncs_v2_space_8'&&get(e,'paddingStart')==='@dimen/ncs_v2_space_24')set(e,{paddingTop:'@dimen/ncs_v2_space_24',clipChildren:'false',clipToPadding:'false'});
  if(e.name==='Space'&&get(e,'layout_weight')){set(e,{layout_height:'16dp'});}
  if(e.name==='TextView'&&get(e,'gravity')==='end')set(e,{textColor:'@color/ncs_v2_primary'});
 }
 if(f.endsWith('_intro.xml')){
  const content=nodes(visual(d))[0], ch=nodes(content);
  ch[0]=includeHeader();
  const heroIndex=ch.findIndex(e=>all(e).some(n=>get(n,'src')==='@drawable/brain'));
  const comparison=false;
  ch[heroIndex]=el('FrameLayout',{layout_width:'match_parent',layout_height:'234dp'},[
   pic('raccoon_welcome','match_parent','210dp',{layout_gravity:'center|bottom'}),
   text('ncs_v2_together','16sp',{layout_gravity:'top|end',gravity:'end',textColor:'@color/ncs_v2_ink'}),
   pic('decor_plus_minus','110dp','104dp',{layout_gravity:'end|center_vertical'})
  ]);
  content.elements=ch;
  for(const e of all(d)){
   if(get(e,'textColor')==='@color/ncs_v2_muted')set(e,{textColor:'@color/ncs_v2_primary'});
   if(get(e,'background')==='@drawable/ncs_v2_panel')set(e,{background:'@drawable/ncs_v2_surface_outline'});
   if(get(e,'text')==='@string/ncs_v2_how_it_works')set(e,{gravity:'start'});
  }
 }
 if(f==='ncs_v2_level_selection.xml'){
  const content=nodes(visual(d))[0];
  for(const e of all(d)){
   if(get(e,'src')==='@drawable/cuub_1')set(e,{src:'@drawable/decor_math_tools',scaleType:'fitCenter',layout_width:'96dp'});
   if(get(e,'text')==='@string/ncs_v2_where_start')set(e,{textColor:'@color/ncs_v2_primary'});
   if(get(e,'layout_height')==='140dp')set(e,{layout_height:'wrap_content',minHeight:'140dp'});
  }
  const ch=nodes(content), i=ch.findIndex(e=>all(e).some(x=>get(x,'src')==='@drawable/girl_2'));
  ch[i]=box([pic('raccoon_levels','160dp','140dp',{layout_gravity:'center_horizontal'}),text('ncs_v2_level_can_reselect','17sp',{gravity:'center',textColor:'@color/ncs_v2_ink'})],{gravity:'center_horizontal'});
  content.elements=ch;
 }
 if(f==='ncs_v2_game.xml'){
  for(const e of all(d)){
   if(get(e,'src')==='@drawable/girl_qes')set(e,{src:'@drawable/raccoon_timer',scaleType:'fitCenter'});
   if(['@string/ncs_v2_tap_correct_number','@string/ncs_v2_great_start'].includes(get(e,'text')))set(e,{textColor:'@color/ncs_v2_primary'});
   if(get(e,'layout_height')==='197dp')set(e,{layout_height:'wrap_content'});
  }
 }
 if(f==='ncs_v2_result_success.xml'||f==='ncs_v2_result_failure.xml'){
  const success=f.includes('success');
  for(const e of all(d)){
   if(['@drawable/ic_smile','@drawable/ic_sad'].includes(get(e,'src')))set(e,{src:'@drawable/raccoon_'+(success?'success':'failure'),scaleType:'fitCenter',layout_width:'154dp',layout_height:'146dp'});
   if(get(e,'src')==='@drawable/kids')set(e,{src:'@drawable/raccoon_'+(success?'happy':'thinking'),scaleType:'fitCenter',layout_width:'180dp',layout_height:'148dp'});
   if(get(e,'textColor')==='@color/ncs_v2_muted')set(e,{textColor:'@color/ncs_v2_primary'});
   if(e.name==='TextView'&&['40dp','44dp'].includes(get(e,'layout_height')))set(e,{minHeight:get(e,'layout_height'),layout_height:'wrap_content'});
   if(get(e,'background')==='@drawable/ncs_v2_panel')set(e,{background:'@drawable/ncs_v2_surface_outline',elevation:'6dp'});
  }
  const c=nodes(visual(d))[0];
  c.elements=nodes(c).map(e=>{if(e.name==='Space')set(e,{layout_height:'12dp'});return e;});
 }
 if(f==='ncs_v2_welcome.xml'){
  set(byText(d,'ncs_v2_small_discovery'),{text:'@string/ncs_v2_welcome_math_description'});
  const content=nodes(visual(d))[0], ch=nodes(content), last=ch[ch.length-1];
  last.elements.unshift(text('ncs_v2_small_discovery','15sp',{gravity:'center'}));
  for(const e of all(d))if(get(e,'src')==='@drawable/raccoon_welcome')set(e,{layout_height:'258dp',adjustViewBounds:null});
 }
 write('layout/'+f,d);
}
// Comparison rules: reuse original labels, example, CTA and ID; flow vertically.
{
 const d=read('layout/ncs_v2_comparison_rules.xml'), old=nodes(visual(d));
 const heading=clearPosition(old[1]), example=clearPosition(old[3]), hint=clearPosition(old[4]), cta=clearPosition(old[5]);
 for(const e of all(heading))if(get(e,'textColor')==='@color/ncs_v2_muted')set(e,{textColor:'@color/ncs_v2_primary'});
 set(example,{background:'@drawable/ncs_v2_surface_outline',padding:'20dp',paddingTop:null,paddingBottom:null,paddingStart:null,paddingEnd:null});
 for(const e of all(example))if(e.name==='TextView'&&get(e,'gravity')==='center')set(e,{gravity:'start'});
 set(hint,{textColor:'@color/ncs_v2_primary'});set(cta,{layout_marginTop:'12dp',clickable:'true',focusable:'true'});
 reflow(d,[includeHeader(),spacer('32dp'),heading,spacer('32dp'),el('FrameLayout',{layout_width:'match_parent',layout_height:'210dp'},[pic('raccoon_thinking','match_parent','210dp'),pic('decor_comparison','110dp','104dp',{layout_gravity:'end|center_vertical'})]),spacer('32dp'),example,spacer('32dp'),hint,cta]);
 write('layout/ncs_v2_comparison_rules.xml',d);
}
// Comparison levels: eliminate percentage heights and absolute supporting text.
{
 const d=read('layout/ncs_v2_comparison_level.xml'), old=nodes(visual(d));
 const toolbar=clearPosition(old[0]), heading=clearPosition(old[1]), list=clearPosition(old[2]), cta=clearPosition(old[4]);
 set(heading,{layout_height:'wrap_content',minHeight:'140dp'});set(list,{layout_height:'wrap_content'});
 for(const e of all(heading)){if(e.name==='ImageView')set(e,{src:'@drawable/decor_math_tools',scaleType:'fitCenter',layout_width:'96dp'});if(get(e,'text')==='@string/ncs_v2_where_start')set(e,{textColor:'@color/ncs_v2_primary'});}
 nodes(list).forEach((e,i)=>set(e,{layout_height:'wrap_content',layout_weight:null,layout_marginTop:i?'12dp':null}));
 for(const e of all(toolbar))if(get(e,'text')==='@string/ncs_v2_brand')set(e,{textColor:'@color/ncs_v2_primary'});
 set(cta,{layout_marginTop:'16dp',clickable:'true',focusable:'true'});
 reflow(d,[toolbar,heading,list,spacer('12dp'),box([pic('raccoon_levels','160dp','140dp',{layout_gravity:'center_horizontal'}),text('ncs_v2_level_can_reselect','17sp',{gravity:'center',textColor:'@color/ncs_v2_ink'})]),cta]);
 write('layout/ncs_v2_comparison_level.xml',d);
}
// Comparison question / feedback are one existing screen with the same IDs.
{
 const d=read('layout/ncs_v2_comparison_game.xml'), old=nodes(visual(d));
 old.forEach(clearPosition);
 const [toolbar,title,expression,instruction,answers,feedback,stats,track,hero,hint]=old;
 set(title,{layout_marginTop:'30dp'});set(expression,{layout_height:'150dp',background:'@drawable/ncs_v2_comparison_expression',layout_marginTop:'18dp'});
 set(instruction,{layout_marginTop:'24dp',textColor:'@color/ncs_v2_ink',textSize:'17sp'});
 set(answers,{layout_height:'112dp',layout_marginTop:'12dp',clipChildren:'false'});
 for(const e of nodes(answers))if(e.name==='TextView')set(e,{elevation:'6dp'});
 set(feedback,{layout_marginTop:'18dp',minHeight:'32dp'});set(stats,{layout_marginTop:'24dp'});set(track,{layout_marginTop:'18dp'});
 set(hero,{src:'@drawable/raccoon_timer',layout_width:'88dp',layout_height:'100dp',layout_gravity:'center_horizontal',layout_marginTop:'24dp'});
 set(hint,{layout_marginTop:'16dp',textColor:'@color/ncs_v2_primary'});
 for(const e of all(stats))if(get(e,'textColor')==='@color/ncs_v2_muted')set(e,{textColor:'@color/ncs_v2_primary'});
 set(findId(d,'ncs_v2_comparison_expression_sign'),{textColor:'@color/ncs_v2_operation_add'});
 reflow(d,old);write('layout/ncs_v2_comparison_game.xml',d);
}
// Comparison result: fixed artwork slots with growing text, scrollable on small phones.
{
 const d=read('layout/ncs_v2_comparison_result.xml'), c=nodes(visual(d))[0], ch=nodes(c);
 const heroBlock=ch[1], metrics=ch[2], lower=ch[3];
 set(heroBlock,{layout_height:'wrap_content',layout_weight:null,paddingTop:'12dp',paddingBottom:'24dp'});
 set(findId(d,'ncs_v2_comparison_result_face'),{layout_height:'146dp',layout_weight:null,maxHeight:null,src:'@{success ? @drawable/raccoon_success : @drawable/raccoon_failure}','tools:src':'@drawable/raccoon_success'});
 for(const e of all(heroBlock))if(e.name==='TextView'&&get(e,'layout_height')==='44dp')set(e,{layout_height:'wrap_content',minHeight:'44dp',textColor:'@color/ncs_v2_primary'});
 set(metrics,{layout_height:'wrap_content',minHeight:'181dp',background:'@drawable/ncs_v2_surface_outline',elevation:'6dp'});
 set(lower,{layout_height:'172dp',layout_weight:null});
 const lowerPic=nodes(lower)[0];set(lowerPic,{layout_width:'180dp',layout_height:'148dp',maxHeight:null,scaleX:null,src:'@{success ? @drawable/raccoon_happy : @drawable/raccoon_thinking}'});
 reflow(d,ch);write('layout/ncs_v2_comparison_result.xml',d);
}
// Shared components: native state selectors and Figma's cool surfaces.
for(const f of ['ncs_v2_level_item.xml','ncs_v2_lesson_item.xml','ncs_v2_equation_item.xml','ncs_v2_timer_item.xml','ncs_v2_progress_item.xml','ncs_v2_answer_item.xml']){
 const d=read('layout/'+f);
 if(f==='ncs_v2_level_item.xml'){
  set(visual(d),{elevation:'6dp',paddingTop:'12dp',paddingBottom:'12dp',clickable:'true',focusable:'true'});
  for(const e of all(d))if(get(e,'text')==='@{enabled ? description : @string/ncs_v2_unavailable}')set(e,{textColor:'@color/ncs_v2_primary'});
 }
 if(f==='ncs_v2_lesson_item.xml')set(visual(d),{elevation:'6dp',paddingTop:'12dp',paddingBottom:'12dp'});
 if(f==='ncs_v2_equation_item.xml')for(const e of all(d)){if(get(e,'background')==='@drawable/ncs_v2_panel')set(e,{background:'@drawable/ncs_v2_surface_outline'});if(get(e,'textColor')==='@color/ncs_v2_muted')set(e,{textColor:'@color/ncs_v2_primary'});}
 if(f==='ncs_v2_timer_item.xml')for(const e of all(d))if(get(e,'textColor')?.includes('ncs_v2_muted'))set(e,{textColor:'@{urgent ? @color/ncs_v2_warm_ink : @color/ncs_v2_primary}'});
 if(f==='ncs_v2_progress_item.xml'){
  set(nodes(visual(d))[0],{paddingEnd:'104dp'});
  const e=findId(d,'ncs_v2_progress_status');set(e,{textColor:get(e,'textColor').replace('@color/ncs_v2_muted','@color/ncs_v2_primary')});
 }
 if(f==='ncs_v2_answer_item.xml')set(visual(d),{elevation:'6dp'});
 write('layout/'+f,d);
}
for(const f of ['ncs_v2_answer_card.xml','ncs_v2_answer_selected.xml','ncs_v2_answer_correct.xml','ncs_v2_answer_wrong.xml','ncs_v2_answer_incorrect.xml','ncs_v2_level_card.xml']){
 const d=read('drawable/'+f);
 for(const e of all(d)){
  if(e.name==='corners')set(e,{radius:'@dimen/ncs_v2_radius_panel'});
  if(e.name==='solid'&&get(e,'color')==='@color/ncs_v2_surface')set(e,{color:'@color/ncs_v2_paper'});
 }
 if(f==='ncs_v2_answer_correct.xml')for(const e of all(d))if(e.name==='solid'||e.name==='stroke')set(e,{color:'@color/ncs_v2_primary'});
 write('drawable/'+f,d);
}
{
 const d=read('drawable/ncs_v2_comparison_expression.xml');for(const e of all(d))if(e.name==='solid'||e.name==='stroke')set(e,{color:'@color/ncs_v2_selected'});write('drawable/ncs_v2_comparison_expression.xml',d);
 const d2=read('values/dimens_v2.xml');const dim=all(d2).find(e=>e.attributes?.name==='ncs_v2_timer_width');dim.elements=[{type:'text',text:'100dp'}];write('values/dimens_v2.xml',d2);
 const colors=read('values/colors_v2.xml');const color=all(colors).find(e=>e.attributes?.name==='ncs_v2_primary_pressed');color.elements=[{type:'text',text:'#0755B8'}];write('values/colors_v2.xml',colors);
}
console.log('XML migration finished. Baseline IDs, data variables and expressions recorded.');



