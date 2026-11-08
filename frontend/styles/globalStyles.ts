import { StyleSheet } from 'react-native';

export const globalStyles = StyleSheet.create({
  screen: {
    flex: 1,
    padding: 24,
    backgroundColor: '#f5f5f5',
    justifyContent: 'center',
    alignItems: 'center',
  },

  title: {
    fontSize: 26,
    fontWeight: '700',
    marginBottom: 8,
    textAlign: 'center',
  },

  subtitle: {
    marginBottom: 24,
    textAlign: 'center',
  },

  logoImage: {
    width: 180,
    height: 180,
    borderRadius: 90,
    marginBottom: 24,
  },
});
