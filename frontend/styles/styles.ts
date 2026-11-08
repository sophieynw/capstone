import { StyleSheet } from 'react-native';

export const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  screenContent: {
    padding: 24,
    gap: 16,
  },
  vStack: {
    flex: 1,
    gap: 12,
  },
  hStack: {
    flex: 1,
    flexDirection: 'row',
    justifyContent: 'center',
    gap: 10,
    padding: 2,
  },
  title: {
    fontSize: 28,
    fontWeight: '700',
  },
  modalScreen: {
    flex: 1,
    backgroundColor: '#f5f5f5',
    padding: 18,
  },
  modalScreenContent: {
    gap: 12,
    paddingBottom: 30,
  },
  modalHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    width: '100%',
    padding: 8,
  },
  modalHeaderActions: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
  },
  modalHeaderButton: {
    marginTop: 0,
  },
  modalMain: {
    flexDirection: 'column',
    padding: 4,
    gap: 12,
  },
  bigCard: {
    width: '100%',
    borderRadius: 32,
    padding: 20,
    gap: 16,
  },
  mediumCard: {
    // width: '100%',
    borderRadius: 28,
    padding: 18,
    gap: 4,
  },
  mediumCardWithAvatar: {
    borderRadius: 28,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  mediumCardWithAvatarLeft: {
    gap: 6,
  },
  mediumCardWithAvatarRight: {
    width: 70,
    height: 70,
  },
  summaryCard: {
    flex: 1,
    minWidth: 0,
    borderRadius: 28,
    padding: 10,
    alignItems: 'center',
    justifyContent: 'center',
    gap: 2,
  },
  summaryCardVisual: {
    height: 30,
    justifyContent: 'center',
  },
  summaryCardAction: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    gap: 2,
  },

  // TODO: remove margins
  formCard: {
    width: '100%',
    backgroundColor: '#ffffff',
    padding: 20,
    borderRadius: 28,
    gap: 10,
  },
  input: {
    borderWidth: 1,
    borderColor: '#cccccc',
    borderRadius: 9999,
    padding: 10,
    paddingLeft: 12,
  },
  inputError: {
    borderColor: '#d32f2f',
  },
  errorText: {
    color: '#d32f2f',
    fontSize: 12,
    marginTop: -8,
    marginBottom: 10,
  },
  roleRow: {
    flexDirection: 'row',
    gap: 10,
    marginTop: 6,
    marginBottom: 12,
  },
  roleButton: {
    flex: 1,
    backgroundColor: '#ffffff',
    padding: 10,
    alignItems: 'center',
    borderWidth: 1,
    borderColor: '#74A985',
    borderRadius: 9999,
  },
  select: {
    borderWidth: 1,
    borderColor: '#cccccc',
    borderRadius: 9999,
    paddingTop: 12,
    paddingBottom: 12,
    marginTop: 6,
    marginBottom: 12,
  },
  button: {
    backgroundColor: '#D8E7DD',
    padding: 10,
    alignItems: 'center',
    borderRadius: 9999,
    borderColor: '#74A985',
    borderWidth: 1,
  },
  buttonText: {
    color: '#365841',
  },
  buttonWarn: {
    backgroundColor: '#FCD0D0',
    padding: 10,
    alignItems: 'center',
    borderRadius: 9999,
    borderColor: '#C96F6F',
    borderWidth: 1,
  },
  buttonTextWarn: {
    color: '#7A2E2E',
  },
  disabledButton: {
    opacity: 0.6,
  },
  link: {
    textAlign: 'center',
    textDecorationLine: 'underline',
    color: '#365841',
  },
});
