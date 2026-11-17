const fs=require('fs'),x=require('C:/Users/Galyaminsky/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/xml-js');
const base='app/src/main/res/',out='diagnostics/enot_phone_polish_before.json';
if(fs.existsSync(out))throw Error('Already applied');
const kids=e=>(e.elements||[]).filter(e=>e.type==='element'),all=e=>[e,...kids(e).flatMap(all)];
const get=(e,k)=>e.attributes?.['android:'+k],set=(e,a)=>{e.attributes||={};for(const[k,v]of Object.entries(a)){if(v===null)delete e.attributes['android:'+k];else e.attributes['android:'+k]=v;}return e;};
const read=f=>x.xml2js(fs.readFileSync(base+f,'utf8'));
const changed=[];const write=(f,d)=>{fs.writeFileSync(base+f,x.js2xml(d,{spaces:4,indentAttributes:true,attributeValueFn:v=>v.replace(/&(?!(?:amp|quot|apos|lt|gt);)/g,'&amp;').replace(/</g,'&lt;')})+'\n');changed.push(base+f);};
const el=(name,a={},elements=[])=>set({type:'element',name,elements},a);
const layouts=fs.readdirSync(base+'layout').filter(f=>f.startsWith('ncs_v2_')&&f.endsWith('.xml'));
const before={};for(const f of layouts){const d=read('layout/'+f);before[f]={ids:all(d).map(e=>get(e,'id')).filter(Boolean),variables:all(d).filter(e=>e.name==='variable').map(e=>e.attributes),bindings:all(d).flatMap(e=>Object.entries(e.attributes||{}).filter(([k,v])=>v.startsWith('@{')).map(([k,v])=>({k,v})))};}fs.writeFileSync(out,JSON.stringify(before,null,2));
// Separate reusable outlines; shared neutral surfaces must stay neutral.
for(const[name,color]of Object.entries({ncs_v2_accent_outline:'ncs_v2_primary',ncs_v2_result_positive_outline:'ncs_v2_success',ncs_v2_result_negative_outline:'ncs_v2_error'})){
 const d=read('drawable/ncs_v2_surface_outline.xml');for(const e of all(d))if(e.name==='stroke')set(e,{color:'@color/'+color,width:'@dimen/ncs_v2_stroke_regular'});write('drawable/'+name+'.xml',d);
}
{
 const d=read('drawable/ncs_v2_secondary_button.xml');for(const e of all(d))if(e.name==='stroke'&&get(e,'color')==='@color/ncs_v2_border')set(e,{color:'@color/ncs_v2_primary',width:'@dimen/ncs_v2_stroke_regular'});
 const def=all(d).find(e=>e.attributes?.['android:drawable']==='@drawable/ncs_v2_surface_outline');delete def.attributes['android:drawable'];def.elements=[el('shape',{shape:'rectangle'},[el('solid',{color:'@color/ncs_v2_paper'}),el('stroke',{width:'@dimen/ncs_v2_stroke_regular',color:'@color/ncs_v2_primary'}),el('corners',{radius:'@dimen/ncs_v2_radius_control'})])];write('drawable/ncs_v2_secondary_button.xml',d);
 const c=read('drawable/ncs_v2_comparison_answer_card.xml');for(const e of all(c))if(e.name==='solid'||e.name==='stroke')set(e,{color:'@color/ncs_v2_success'});write('drawable/ncs_v2_comparison_answer_card.xml',c);
 const w=read('drawable/ncs_v2_answer_wrong.xml');for(const e of all(w))if(e.name==='solid')set(e,{color:'@color/ncs_v2_error'});write('drawable/ncs_v2_answer_wrong.xml',w);
 const expr=read('drawable/ncs_v2_comparison_expression.xml');for(const e of all(expr))if(e.name==='solid'||e.name==='stroke')set(e,{color:'@color/ncs_v2_paper'});write('drawable/ncs_v2_comparison_expression.xml',expr);
}
for(const f of layouts){const d=read('layout/'+f),r=kids(d)[0],scroll=kids(r).find(e=>e.name==='ScrollView');
 // Every ancestor of an elevated answer must allow its shadow to escape.
 for(const e of all(d))if(['LinearLayout','FrameLayout','ScrollView'].includes(e.name))set(e,{clipChildren:'false',clipToPadding:'false'});
 if(scroll){set(scroll,{scrollbars:'none'});const content=kids(scroll)[0];
  set(content,{paddingTop:'@dimen/ncs_v2_space_12',paddingBottom:'@dimen/ncs_v2_space_12'});
  if(get(content,'padding'))set(content,{padding:null,paddingStart:'24dp',paddingEnd:'24dp'});
  for(const e of all(content)){
   if(e.name==='Space')set(e,{layout_height:'0dp'});
   if(['24dp','32dp','@dimen/ncs_v2_space_32','@dimen/ncs_v2_space_24'].includes(get(e,'layout_marginTop')))set(e,{layout_marginTop:'@dimen/ncs_v2_space_8'});
   if(get(e,'minHeight')==='140dp')set(e,{minHeight:null,paddingTop:'8dp',paddingBottom:'8dp'});
   if(get(e,'paddingTop')==='@dimen/ncs_v2_space_24')set(e,{paddingTop:'8dp'});
   if(get(e,'paddingBottom')==='@dimen/ncs_v2_space_24')set(e,{paddingBottom:'8dp'});
  }
  // Only illustration/support slots flex. Controls and level cards retain their sizes.
  for(const e of kids(content)){
   const pics=all(e).filter(n=>n.name==='ImageView');
   const flexible=e.name==='ImageView'||(pics.length&&e.name==='FrameLayout'&&!all(e).some(n=>get(n,'id')?.includes('expression')))||pics.some(n=>get(n,'src')==='@drawable/raccoon_levels');
   if(flexible&&!(e.name==='FrameLayout'&&all(e).some(n=>n.attributes?.layout==='@layout/ncs_v2_brand_header'))){
    set(e,{layout_height:'@dimen/ncs_v2_touch_target_min',layout_weight:'1'});
    for(const p of pics)if(p!==e)set(p,{layout_height:'match_parent',scaleType:'fitCenter'});
    if(e.name==='LinearLayout'&&pics.some(p=>get(p,'src')==='@drawable/raccoon_levels')){
     set(e,{layout_height:'wrap_content',layout_weight:null});for(const p of pics)set(p,{layout_height:'48dp'});
    }
   }
  }
  // Welcome artwork and comparison result group contain text beneath the image.
  for(const e of kids(content)){
   if(e.name!=='LinearLayout')continue;
   const art=kids(e).find(n=>n.name==='FrameLayout'&&all(n).some(p=>get(p,'src')==='@drawable/raccoon_welcome'||get(p,'id')==='@+id/ncs_v2_comparison_result_face'));
   if(art){set(e,{layout_height:'wrap_content',layout_weight:'2'});set(art,{layout_height:'48dp',layout_weight:'1'});for(const p of all(art))if(p.name==='ImageView')set(p,{layout_height:'match_parent'});}
  }
 }
 for(const e of all(d)){
  if(e.name==='ImageView'&&get(e,'src')==='@drawable/decor_sparkles'&&get(e,'layout_width')==='44dp'&&!f.includes('result'))set(e,{layout_width:'88dp',layout_height:'88dp'});
  if(e.name==='ImageView'&&get(e,'src')==='@drawable/decor_math_tools')set(e,{layout_width:'@dimen/ncs_v2_back_width',layout_height:'88dp',scaleType:'fitCenter'});
  if(get(e,'background')==='@drawable/ncs_v2_surface_outline'&&(f.endsWith('_intro.xml')||f==='ncs_v2_comparison_rules.xml'||f==='ncs_v2_equation_item.xml'))set(e,{background:'@drawable/ncs_v2_accent_outline'});
  if(f==='ncs_v2_comparison_game.xml'&&get(e,'id')==='@+id/ncs_v2_comparison_expression_sign')set(e,{textColor:'@color/ncs_v2_primary'});
  if(f==='ncs_v2_comparison_rules.xml'&&get(e,'text')==='@string/ncs_v2_comparison_rules_example_sign')set(e,{textColor:'@color/ncs_v2_primary',textSize:'@dimen/ncs_v2_text_36'});
  if(get(e,'background')==='@drawable/ncs_v2_surface_outline'&&f.includes('result'))set(e,{background:f==='ncs_v2_comparison_result.xml'?'@{success ? @drawable/ncs_v2_result_positive_outline : @drawable/ncs_v2_result_negative_outline}':'@drawable/ncs_v2_result_'+(f.includes('success')?'positive':'negative')+'_outline'});
  if(f==='ncs_v2_game.xml'&&get(e,'src')==='@drawable/raccoon_timer')set(e,{layout_width:'88dp',layout_height:'match_parent',scaleType:'fitCenter'});
 }
 if(f.includes('result'))for(const e of all(d))if(e.name==='FrameLayout'){
  const star=kids(e).find(n=>get(n,'src')==='@drawable/decor_sparkles');if(star){const left=JSON.parse(JSON.stringify(star));set(left,{layout_gravity:'start|center_vertical'});e.elements.push(left);}
 }
 write('layout/'+f,d);
}
// Compact answer height uses the existing 72dp progress component geometry.
const dims=read('values/dimens_v2.xml');for(const e of all(dims))if(e.attributes?.name==='ncs_v2_answer_height')e.elements=[{type:'text',text:'@dimen/ncs_v2_progress_height'}];write('values/dimens_v2.xml',dims);
fs.writeFileSync('diagnostics/enot_phone_polish_files.json',JSON.stringify(changed,null,2));
console.log('Applied phone polish to '+changed.length+' resource files.');
