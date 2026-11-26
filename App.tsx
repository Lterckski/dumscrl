import { LinearGradient } from 'expo-linear-gradient';
import { StatusBar } from 'expo-status-bar';
import { useEffect, useRef, useState } from 'react';
import { AccessibilityInfo, Animated, StyleSheet, View } from 'react-native';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { useEngine } from './src/engine';
import { Camera } from './src/screens/Camera';
import { Home } from './src/screens/Home';
import { Settings } from './src/screens/Settings';
import { Signs } from './src/screens/Signs';
import { TabBar, type Tab } from './src/TabBar';
import { applyTheme, savedTheme, spring, useColors } from './src/theme';

applyTheme(savedTheme()); // before the first frame, so there's no flash of the wrong theme

export default function App() {
  const c = useColors();
  const engine = useEngine();
  const [tab, setTab] = useState<Tab>('home');

  // New page eases in from slightly below; with Reduce motion it only fades.
  const enter = useRef(new Animated.Value(1)).current;
  const [reduceMotion, setReduceMotion] = useState(false);
  useEffect(() => {
    AccessibilityInfo.isReduceMotionEnabled().then(setReduceMotion);
  }, []);
  const go = (t: Tab) => {
    if (t === tab) return;
    setTab(t);
    enter.setValue(0);
    Animated.spring(enter, { toValue: 1, ...spring }).start();
  };

  const page = {
    flex: 1,
    opacity: enter,
    transform: reduceMotion ? [] : [{ translateY: enter.interpolate({ inputRange: [0, 1], outputRange: [12, 0] }) }],
  };

  return (
    <SafeAreaProvider>
      <View style={styles.root}>
        <LinearGradient colors={c.backdrop} locations={[0, 0.4, 0.7, 1]} start={{ x: 0, y: 0 }} end={{ x: 1, y: 1 }} style={StyleSheet.absoluteFill} />
        <StatusBar style="auto" />
        <Animated.View style={page}>
          {tab === 'home' && <Home engine={engine} />}
          {tab === 'camera' && <Camera engine={engine} visible />}
          {tab === 'signs' && <Signs current={engine.state.running ? engine.state.sign : ''} />}
          {tab === 'settings' && <Settings engine={engine} />}
        </Animated.View>
        <TabBar tab={tab} onChange={go} />
      </View>
    </SafeAreaProvider>
  );
}

const styles = StyleSheet.create({ root: { flex: 1 } });
