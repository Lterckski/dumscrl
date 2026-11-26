import * as Haptics from 'expo-haptics';
import { useRef, type ReactNode } from 'react';
import { Animated, Pressable, type PressableProps, type StyleProp, type ViewStyle } from 'react-native';
import { spring } from './theme';

type Props = Omit<PressableProps, 'style' | 'children'> & {
  style?: StyleProp<ViewStyle>;
  children: ReactNode;
  /** How far it shrinks under the finger. */
  scaleTo?: number;
  /** Layout for the touch area itself (e.g. flex: 1 in a row). */
  containerStyle?: StyleProp<ViewStyle>;
};

/** A button that squishes the instant it's touched (not on release) and ticks. */
export function Press({ style, containerStyle, children, scaleTo = 0.96, onPressIn, onPressOut, disabled, ...rest }: Props) {
  const scale = useRef(new Animated.Value(1)).current;
  const to = (v: number) => Animated.spring(scale, { toValue: v, ...spring }).start();
  return (
    <Pressable
      {...rest}
      disabled={disabled}
      hitSlop={8}
      style={containerStyle}
      onPressIn={(e) => {
        to(scaleTo);
        Haptics.selectionAsync();
        onPressIn?.(e);
      }}
      onPressOut={(e) => {
        to(1);
        onPressOut?.(e);
      }}
    >
      <Animated.View style={[style, { transform: [{ scale }] }, disabled && { opacity: 0.4 }]}>{children}</Animated.View>
    </Pressable>
  );
}
