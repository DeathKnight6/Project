<script setup lang="ts">
import loginPicture from '../../Picture/老实赚钱很枯燥吧.png'
</script>

<template>
  <div class="auth-shell">
    <section class="auth-art">
      <router-link to="/login" class="auth-brand"><span aria-hidden="true">股</span>模拟股票交易系统</router-link>
      <div class="poster-scene">
        <h2>开门见财</h2>
        <div class="picture-mount"><img :src="loginPicture" alt="老实赚钱，很枯燥吧？" /></div>
        <div class="poster-bottom" aria-hidden="true"><span></span><i></i><span></span></div>
      </div>
      <div class="lantern lantern-left" aria-hidden="true"><i></i></div>
      <div class="lantern lantern-right" aria-hidden="true"><i></i></div>
    </section>
    <section class="auth-form-panel"><div class="auth-card"><slot /></div></section>
  </div>
</template>

<style scoped>
.auth-shell { min-height: 100vh; display: grid; grid-template-columns: minmax(0, 1.4fr) minmax(420px, 1fr); background: var(--paper); }
.auth-art { position: relative; display: flex; flex-direction: column; min-height: 100vh; padding: 42px 52px; overflow: hidden; background: var(--red) url('../assets/gold-clouds.svg') center / cover; border-right: 8px solid var(--gold); }
.auth-brand { position: relative; z-index: 2; display: flex; align-items: center; gap: 18px; width: fit-content; color: #fff; font-family: 'SimSun', serif; font-size: 28px; font-weight: 700; letter-spacing: 5px; text-decoration: none; }
.auth-brand > span { display: grid; place-items: center; width: 41px; height: 41px; font-size: 29px; background: var(--gold); color: #000; outline: 1px solid var(--gold); outline-offset: 4px; }
.poster-scene { width: 100%; max-width: 760px; margin: auto; padding: 46px 0 36px; position: relative; z-index: 1; }
.poster-scene h2 { margin: 0 0 38px; color: #fff; font-size: clamp(38px, 4vw, 66px); text-align: center; letter-spacing: .3em; padding-left: .3em; font-weight: 700; }
.picture-mount { padding: 9px; background: var(--gold); border: 1px solid #f3d78d; outline: 1px solid var(--gold); outline-offset: 6px; }
.picture-mount img { display: block; width: 100%; height: auto; }
.poster-bottom { display: flex; align-items: center; gap: 16px; padding: 37px 8px 0; }
.poster-bottom span { height: 1px; flex: 1; background: var(--gold); }.poster-bottom i { width: 10px; height: 10px; border: 1px solid var(--gold); transform: rotate(45deg); }
.auth-form-panel { display: flex; align-items: center; justify-content: center; padding: 58px 56px; background: url('../assets/gold-clouds.svg') 50% 50% / 1050px auto; }
.auth-card { width: 100%; max-width: 360px; position: relative; padding-block: 26px; }
.lantern { position: absolute; top: 0; width: 51px; height: 105px; border-bottom: 1px solid var(--gold); }
.lantern::before { content: ''; position: absolute; left: 25px; top: 0; height: 46px; width: 1px; background: var(--gold); }
.lantern::after { content: ''; position: absolute; top: 46px; left: 0; width: 49px; height: 58px; border: 1px solid var(--gold); border-radius: 40%; background: var(--red); box-shadow: inset 12px 0 0 -11px var(--gold), inset -12px 0 0 -11px var(--gold); }
.lantern i { position: absolute; top: 105px; left: 25px; width: 1px; height: 32px; background: var(--gold); }
.lantern-left { right: 120px; }.lantern-right { right: 38px; transform: translateY(-20px); }
@media(max-width:1100px) { .auth-art { padding: 36px; }.auth-form-panel { padding: 46px 38px; }.auth-shell { grid-template-columns: minmax(0, 1.2fr) minmax(380px, 1fr); }.auth-brand { font-size: 25px; }.lantern-left { display: none; } }
@media(max-width:760px) {
  .auth-shell { grid-template-columns: 1fr; }.auth-art { min-height: 0; padding: 26px 24px 30px; border-right: 0; border-bottom: 6px solid var(--gold); }
  .auth-brand { font-size: 20px; letter-spacing: 1px; }.auth-brand > span { width: 32px; height: 32px; font-size: 24px; }.poster-scene { padding: 32px 0 0; max-width: 440px; }.poster-scene h2 { display: none; }.poster-bottom { display: none; }.picture-mount { padding: 5px; outline-offset: 4px; }.auth-form-panel { padding: 36px 28px 42px; }.auth-card { padding: 0; }.lantern-right { right: 24px; transform: scale(.7) translateY(-22px); transform-origin: top; }
}
</style>
