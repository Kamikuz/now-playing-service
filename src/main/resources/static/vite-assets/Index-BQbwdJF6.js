import{c as le,r as o,j as e,u as ce,a as ue,b as de,d as me,T as fe}from"./index-CDLYXOKS.js";import{b as B}from"./chunk-WBUKVQRU-CX9Zl0ci.js";import{u as V,m as Y,a as $,b as X,c as K,d as G}from"./chunk-TW2E3XVA-pBIxJ7Bv.js";import{R as pe,M as Z,r as J,a as Q}from"./index-Bgo4WT-8.js";import"./vs2015-D9705uCu.js";import{C as he}from"./version-DL7PLCV6.js";import{V as S,S as xe,O as ve,P as ge,a as ee,b as we,M as be,W as ye,c as je,C as Ce}from"./three.module-B_9urBBX.js";import"./chunk-6VC6TS2O-U6b5-wwT.js";import"./index-BoaTOKwE.js";import"./index-weEymvJi.js";import"./chunk-736YWA4T-DCpY4-Mx.js";import"./useDialog-CTdkVp9L.js";import"./useOverlayTriggerState-CKSYWNNA.js";const Ne=[["path",{d:"M11 6a13 13 0 0 0 8.4-2.8A1 1 0 0 1 21 4v12a1 1 0 0 1-1.6.8A13 13 0 0 0 11 14H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2z",key:"q8bfy3"}],["path",{d:"M6 14a12 12 0 0 0 2.4 7.2 2 2 0 0 0 3.2-2.4A8 8 0 0 1 10 14",key:"1853fq"}],["path",{d:"M8 6v8",key:"15ugcq"}]],Re=le("megaphone",Ne),L=8,Me=`
#define MAX_COLORS ${L}
uniform vec2 uCanvas;
uniform float uTime;
uniform float uSpeed;
uniform vec2 uRot;
uniform int uColorCount;
uniform vec3 uColors[MAX_COLORS];
uniform int uTransparent;
uniform float uScale;
uniform float uFrequency;
uniform float uWarpStrength;
uniform vec2 uPointer; // in NDC [-1,1]
uniform float uMouseInfluence;
uniform float uParallax;
uniform float uNoise;
uniform int uIterations;
uniform float uIntensity;
uniform float uBandWidth;
varying vec2 vUv;

void main() {
  float t = uTime * uSpeed;
  vec2 p = vUv * 2.0 - 1.0;
  p += uPointer * uParallax * 0.1;
  vec2 rp = vec2(p.x * uRot.x - p.y * uRot.y, p.x * uRot.y + p.y * uRot.x);
  vec2 q = vec2(rp.x * (uCanvas.x / uCanvas.y), rp.y);
  q /= max(uScale, 0.0001);
  q /= 0.5 + 0.2 * dot(q, q);
  q += 0.2 * cos(t) - 7.56;
  vec2 toward = (uPointer - rp);
  q += toward * uMouseInfluence * 0.2;

    for (int j = 0; j < 5; j++) {
      if (j >= uIterations - 1) break;
      vec2 rr = sin(1.5 * (q.yx * uFrequency) + 2.0 * cos(q * uFrequency));
      q += (rr - q) * 0.15;
    }

    vec3 col = vec3(0.0);
    float a = 1.0;

    if (uColorCount > 0) {
      vec2 s = q;
      vec3 sumCol = vec3(0.0);
      float cover = 0.0;
      for (int i = 0; i < MAX_COLORS; ++i) {
            if (i >= uColorCount) break;
            s -= 0.01;
            vec2 r = sin(1.5 * (s.yx * uFrequency) + 2.0 * cos(s * uFrequency));
            float m0 = length(r + sin(5.0 * r.y * uFrequency - 3.0 * t + float(i)) / 4.0);
            float kBelow = clamp(uWarpStrength, 0.0, 1.0);
            float kMix = pow(kBelow, 0.3); // strong response across 0..1
            float gain = 1.0 + max(uWarpStrength - 1.0, 0.0); // allow >1 to amplify displacement
            vec2 disp = (r - s) * kBelow;
            vec2 warped = s + disp * gain;
            float m1 = length(warped + sin(5.0 * warped.y * uFrequency - 3.0 * t + float(i)) / 4.0);
            float m = mix(m0, m1, kMix);
            float w = 1.0 - exp(-uBandWidth / exp(uBandWidth * m));
            sumCol += uColors[i] * w;
            cover = max(cover, w);
      }
      col = clamp(sumCol, 0.0, 1.0);
      a = uTransparent > 0 ? cover : 1.0;
    } else {
        vec2 s = q;
        for (int k = 0; k < 3; ++k) {
            s -= 0.01;
            vec2 r = sin(1.5 * (s.yx * uFrequency) + 2.0 * cos(s * uFrequency));
            float m0 = length(r + sin(5.0 * r.y * uFrequency - 3.0 * t + float(k)) / 4.0);
            float kBelow = clamp(uWarpStrength, 0.0, 1.0);
            float kMix = pow(kBelow, 0.3);
            float gain = 1.0 + max(uWarpStrength - 1.0, 0.0);
            vec2 disp = (r - s) * kBelow;
            vec2 warped = s + disp * gain;
            float m1 = length(warped + sin(5.0 * warped.y * uFrequency - 3.0 * t + float(k)) / 4.0);
            float m = mix(m0, m1, kMix);
            col[k] = 1.0 - exp(-uBandWidth / exp(uBandWidth * m));
        }
        a = uTransparent > 0 ? max(max(col.r, col.g), col.b) : 1.0;
    }

    col *= uIntensity;

    if (uNoise > 0.0001) {
      float n = fract(sin(dot(gl_FragCoord.xy + vec2(uTime), vec2(12.9898, 78.233))) * 43758.5453123);
      col += (n - 0.5) * uNoise;
      col = clamp(col, 0.0, 1.0);
    }

    vec3 rgb = (uTransparent > 0) ? col * a : col;
    gl_FragColor = vec4(rgb, a);
}
`,ke=`
varying vec2 vUv;
void main() {
  vUv = uv;
  gl_Position = vec4(position, 1.0);
}
`;function Se({className:y,style:r,rotation:p=90,speed:d=.2,colors:E=[],transparent:h=!0,autoRotate:l=0,scale:O=1,frequency:x=1,warpStrength:q=1,mouseInfluence:j=1,parallax:C=.5,noise:N=.15,iterations:R=1,intensity:M=1.5,bandWidth:f=6}){const v=o.useRef(null),c=o.useRef(null),I=o.useRef(null),P=o.useRef(null),A=o.useRef(null),_=o.useRef(p),z=o.useRef(l),F=o.useRef(new S(0,0)),ne=o.useRef(new S(0,0)),re=o.useRef(8);return o.useEffect(()=>{const t=v.current,u=new xe,b=new ve(-1,1,1,-1,0,1),m=new ge(2,2),a=Array.from({length:L},()=>new ee(0,0,0)),n=new we({vertexShader:ke,fragmentShader:Me,uniforms:{uCanvas:{value:new S(1,1)},uTime:{value:0},uSpeed:{value:d},uRot:{value:new S(1,0)},uColorCount:{value:0},uColors:{value:a},uTransparent:{value:h?1:0},uScale:{value:O},uFrequency:{value:x},uWarpStrength:{value:q},uPointer:{value:new S(0,0)},uMouseInfluence:{value:j},uParallax:{value:C},uNoise:{value:N},uIterations:{value:R},uIntensity:{value:M},uBandWidth:{value:f}},premultipliedAlpha:!0,transparent:!0});P.current=n;const g=new be(m,n);u.add(g);const s=new ye({antialias:!1,powerPreference:"high-performance",alpha:!0});c.current=s,s.outputColorSpace=je,s.setPixelRatio(Math.min(window.devicePixelRatio||1,2)),s.setClearColor(0,h?0:1),s.domElement.style.width="100%",s.domElement.style.height="100%",s.domElement.style.display="block",t.appendChild(s.domElement);const W=new Ce,T=()=>{const w=t.clientWidth||1,k=t.clientHeight||1;s.setSize(w,k,!1),n.uniforms.uCanvas.value.set(w,k)};if(T(),"ResizeObserver"in window){const w=new ResizeObserver(T);w.observe(t),A.current=w}else window.addEventListener("resize",T);const U=()=>{const w=W.getDelta(),k=W.elapsedTime;n.uniforms.uTime.value=k;const D=(_.current%360+z.current*k)*Math.PI/180,oe=Math.cos(D),se=Math.sin(D);n.uniforms.uRot.value.set(oe,se);const H=ne.current,ae=F.current,ie=Math.min(1,w*re.current);H.lerp(ae,ie),n.uniforms.uPointer.value.copy(H),s.render(u,b),I.current=requestAnimationFrame(U)};return I.current=requestAnimationFrame(U),()=>{I.current!==null&&cancelAnimationFrame(I.current),A.current?A.current.disconnect():window.removeEventListener("resize",T),m.dispose(),n.dispose(),s.dispose(),s.forceContextLoss(),s.domElement&&s.domElement.parentElement===t&&t.removeChild(s.domElement)}},[]),o.useEffect(()=>{const t=P.current,u=c.current;if(!t)return;_.current=p,z.current=l,t.uniforms.uSpeed.value=d,t.uniforms.uScale.value=O,t.uniforms.uFrequency.value=x,t.uniforms.uWarpStrength.value=q,t.uniforms.uMouseInfluence.value=j,t.uniforms.uParallax.value=C,t.uniforms.uNoise.value=N,t.uniforms.uIterations.value=R,t.uniforms.uIntensity.value=M,t.uniforms.uBandWidth.value=f;const b=a=>{const n=a.replace("#","").trim(),g=n.length===3?[parseInt(n[0]+n[0],16),parseInt(n[1]+n[1],16),parseInt(n[2]+n[2],16)]:[parseInt(n.slice(0,2),16),parseInt(n.slice(2,4),16),parseInt(n.slice(4,6),16)];return new ee(g[0]/255,g[1]/255,g[2]/255)},m=(E||[]).filter(Boolean).slice(0,L).map(b);for(let a=0;a<L;a++){const n=t.uniforms.uColors.value[a];a<m.length?n.copy(m[a]):n.set(0,0,0)}t.uniforms.uColorCount.value=m.length,t.uniforms.uTransparent.value=h?1:0,u&&u.setClearColor(0,h?0:1)},[p,l,d,O,x,q,j,C,N,R,M,f,E,h]),o.useEffect(()=>{const t=P.current,u=v.current;if(!t||!u)return;const b=m=>{const a=u.getBoundingClientRect(),n=(m.clientX-a.left)/(a.width||1)*2-1,g=-((m.clientY-a.top)/(a.height||1)*2-1);F.current.set(n,g)};return u.addEventListener("pointermove",b),()=>{u.removeEventListener("pointermove",b)}},[]),e.jsx("div",{ref:v,className:`w-full h-full relative overflow-hidden ${y}`,style:r})}const i={MINUTE:60*1e3,HOUR:3600*1e3,DAY:1440*60*1e3,WEEK:10080*60*1e3,MONTH:720*60*60*1e3,YEAR:365*24*60*60*1e3};function te(y){const r=Date.now()-y;return r<i.MINUTE?"刚刚":r<i.HOUR?`${Math.floor(r/i.MINUTE)} 分钟前`:r<i.DAY?`${Math.floor(r/i.HOUR)} 小时前`:r<i.WEEK?`${Math.floor(r/i.DAY)} 天前`:r<i.MONTH?`${Math.floor(r/i.WEEK)} 星期前`:r<i.YEAR?`${Math.floor(r/i.MONTH)} 个月前`:`${Math.floor(r/i.YEAR)} 年前`}function De(){const{isDesktop:y}=ce(),{openExternalUrl:r}=ue(),[p]=de();p.has("showAnnouncement"),p.has("showUpdate");const[d,E]=o.useState(!1),h=me(),[l,O]=o.useState({current:he,isLatest:!0,latestTimestamp:Date.now()}),[x,q]=o.useState({title:"",timestamp:Date.now(),content:""}),{isOpen:j,onOpenChange:C}=V(),{isOpen:N,onOpenChange:R}=V(),M=()=>{document.body.classList.add("fade-out"),setTimeout(()=>{h("/settings/general"),setTimeout(()=>{document.body.classList.contains("fade-out")&&window.location.reload()},300)},500)};return e.jsxs(e.Fragment,{children:[y&&e.jsx(fe,{autoHide:!1}),e.jsxs("div",{children:[e.jsx("style",{children:`
          :root {
            --button-color: #de40ff;
          }

          body {
            background: #0f0f0f;
          }

          #title {
            font-size: 60px;
            color: white;
            user-select: none;
            line-height: 3rem;
            z-index: 5;
            transform: translateY(-10px);
          }

          #now-playing-text {
            color: #ffffff;
            font-weight: normal;
            margin: 0 0.75rem;
            user-select: none;
          }

          body.fade-out {
            animation: dissolve 0.5s forwards;
          }

          @keyframes dissolve {
            0% {
              filter: blur(0) brightness(1) hue-rotate(0deg) saturate(100%) contrast(100%) drop-shadow(0 0 0 rgba(255, 255, 255, 0));
            }

            25% {
              filter: blur(2px) brightness(1.8) hue-rotate(30deg) saturate(125%) contrast(125%) drop-shadow(0 0 6px #ff00ff);
            }

            50% {
              filter: blur(8px) brightness(2.2) hue-rotate(0deg) saturate(150%) contrast(150%) drop-shadow(0 0 12px #00ffff);
            }

            75% {
              filter: blur(12px) brightness(1.2) hue-rotate(-30deg) saturate(100%) contrast(100%) drop-shadow(0 0 16px #ffff00);
            }

            100% {
              filter: blur(20px) brightness(0) hue-rotate(0deg) saturate(0%) contrast(100%) drop-shadow(0 0 0 rgba(255, 255, 255, 0));
            }
          }

          #current-version-div, #update-text {
            user-select: none;
          }

          /* 动画按钮样式 */
          .animated-button {
            position: relative;
            display: flex;
            align-items: center;
            gap: 4px;
            padding: 16px 36px;
            border: 4px solid;
            border-color: transparent;
            font-size: 16px;
            background-color: inherit;
            border-radius: 100px;
            font-weight: 600;
            color: var(--button-color);
            box-shadow: 0 0 0 2px var(--button-color);
            cursor: pointer;
            overflow: hidden;
            transition: all 0.6s cubic-bezier(0.23, 1, 0.32, 1);
            mix-blend-mode: plus-lighter;
            filter: brightness(2.0) saturate(1.2);
            transform: translateY(-10px);
          }
          .animated-button svg {
            position: absolute;
            width: 24px;
            fill: var(--button-color);
            z-index: 9;
            transition: all 0.8s cubic-bezier(0.23, 1, 0.32, 1);
          }
          .animated-button .arr-1 {
            right: 16px;
          }
          .animated-button .arr-2 {
            left: -25%;
          }
          .animated-button .button-circle {
            position: absolute;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%);
            width: 20px;
            height: 20px;
            background-color: var(--button-color);
            border-radius: 50%;
            opacity: 0;
            transition: all 0.8s cubic-bezier(0.23, 1, 0.32, 1);
          }
          .animated-button .button-text {
            position: relative;
            z-index: 1;
            transform: translateX(-12px);
            transition: all 0.8s cubic-bezier(0.23, 1, 0.32, 1);
          }
          .animated-button:hover {
            box-shadow: 0 0 0 12px transparent;
            color: #0f0f0f;
            border-radius: 100px;
          }
          .animated-button:hover .arr-1 {
            right: -25%;
          }
          .animated-button:hover .arr-2 {
            left: 16px;
          }
          .animated-button:hover .button-text {
            transform: translateX(12px);
          }
          .animated-button:hover svg {
            fill: #0f0f0f;
          }
          .animated-button:active {
            scale: 0.95;
            box-shadow: 0 0 0 4px var(--button-color);
          }
          .animated-button:hover .button-circle {
            width: 220px;
            height: 220px;
            opacity: 1;
          }
        `}),e.jsx("div",{"data-overlay-container":"true",children:e.jsxs("main",{children:[e.jsx("div",{children:e.jsx("div",{className:"absolute bottom-0 top-0 flex h-screen w-full flex-col",children:e.jsx("div",{className:"relative flex flex-col gap-20 text-white md:gap-10",children:e.jsxs("div",{className:"flex h-screen w-full items-center justify-center relative overflow-hidden",children:[e.jsxs("div",{className:"flex flex-col items-center gap-10",id:"main-content",children:[e.jsx("div",{className:"absolute top-0 w-full h-full z-0 bg-[#060010]",id:"bg-container",children:e.jsx(Se,{colors:["#a855f7"],rotation:90,speed:.2,scale:1,frequency:1,warpStrength:1,mouseInfluence:1,noise:.15,parallax:.5,iterations:1,intensity:1.5,bandWidth:6,transparent:!0,autoRotate:0})}),e.jsx("div",{className:"flex flex-col items-center",children:e.jsxs("h2",{className:"font-sourcehan text-center text-3xl lg:text-4xl md:text-3xl",id:"title",children:["欢迎使用",e.jsx("span",{className:"px-2 font-dela",id:"now-playing-text",children:"Now Playing"}),"服务"]})}),e.jsxs("button",{className:"animated-button",onClick:M,children:[e.jsx("svg",{className:"arr-2",viewBox:"0 0 24 24",xmlns:"http://www.w3.org/2000/svg",children:e.jsx("path",{d:"M16.1716 10.9999L10.8076 5.63589L12.2218 4.22168L20 11.9999L12.2218 19.778L10.8076 18.3638L16.1716 12.9999H4V10.9999H16.1716Z"})}),e.jsx("span",{className:"button-text",children:"前往设置"}),e.jsx("span",{className:"button-circle"}),e.jsx("svg",{className:"arr-1",viewBox:"0 0 24 24",xmlns:"http://www.w3.org/2000/svg",children:e.jsx("path",{d:"M16.1716 10.9999L10.8076 5.63589L12.2218 4.22168L20 11.9999L12.2218 19.778L10.8076 18.3638L16.1716 12.9999H4V10.9999H16.1716Z"})})]})]}),e.jsxs("div",{style:{position:"fixed",bottom:"2.0rem",width:"100%",textAlign:"center"},children:[e.jsxs("div",{className:"font-poppins",id:"current-version-div",style:{marginBottom:"0.2rem"},children:["版本号：",l.current," (Lilith 弹幕机 修改版本)"]}),e.jsx("div",{className:"font-poppins",id:"update-text",children:l.latest&&e.jsxs("a",{className:"cursor-pointer",onClick:()=>{r("https://gitee.com/widdit/now-playing/releases")},children:["新版本可用：",l.latest]})})]})]})})})}),e.jsx(Y,{size:"xl",isDismissable:!1,scrollBehavior:"inside",hideCloseButton:!0,isOpen:j,onOpenChange:C,className:"px-3 py-2",children:e.jsx($,{className:"font-poppins",children:f=>e.jsxs(e.Fragment,{children:[e.jsxs(X,{className:"flex justify-between items-center",children:[e.jsxs("div",{className:"flex items-center gap-2",children:[e.jsx("div",{className:"breathing-bg flex h-9 w-9 items-center justify-center rounded-full bg-[#15283c]",children:e.jsx(pe,{size:20,strokeWidth:2,color:"#0485f7"})}),l.latest," 新版本可用"]}),e.jsx("div",{className:"font-normal text-sm text-default-500",children:te(l.latestTimestamp)})]}),e.jsx(K,{children:e.jsx("div",{className:"markdown-body",children:e.jsx(Z,{rehypePlugins:[J,Q],components:{img:({node:v,...c})=>e.jsx("img",{...c,referrerPolicy:"no-referrer",className:"max-w-full h-auto rounded-lg my-2"}),a:({node:v,...c})=>e.jsx("a",{...c,className:"text-primary hover:underline",target:"_blank",rel:"noopener noreferrer"})},children:l.updateLog})})}),e.jsxs(G,{children:[e.jsx(B,{color:"default",variant:"flat",onPress:f,children:"取消"}),e.jsx(B,{color:"primary",onPress:()=>{f(),r("https://gitee.com/widdit/now-playing/releases")},children:"确定"})]})]})})}),e.jsx(Y,{size:"xl",isDismissable:!1,isKeyboardDismissDisabled:!0,scrollBehavior:"inside",hideCloseButton:!0,isOpen:N,onOpenChange:R,className:"px-3 py-2",children:e.jsx($,{className:"font-poppins",children:f=>e.jsxs(e.Fragment,{children:[e.jsxs(X,{className:"flex justify-between items-center",children:[e.jsxs("div",{className:"flex items-center gap-2",children:[e.jsx("div",{className:"breathing-bg flex h-9 w-9 items-center justify-center rounded-full bg-[#15283c]",children:e.jsx(Re,{size:20,color:"#0485f7"})}),x.title]}),e.jsx("div",{className:"font-normal text-sm text-default-500",children:te(x.timestamp)})]}),e.jsx(K,{children:e.jsx("div",{className:"markdown-body",children:e.jsx(Z,{rehypePlugins:[J,Q],components:{img:({node:v,...c})=>e.jsx("img",{...c,referrerPolicy:"no-referrer",className:"max-w-full h-auto rounded-lg my-2"}),a:({node:v,...c})=>e.jsx("a",{...c,className:"text-primary hover:underline",target:"_blank",rel:"noopener noreferrer"})},children:x.content})})}),e.jsx(G,{children:e.jsx(B,{color:"primary",onPress:()=>{f(),E(!0)},children:"确定"})})]})})})]})})]})]})}export{De as default};
