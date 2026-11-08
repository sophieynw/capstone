import { Controller, useForm } from 'react-hook-form';
import { Pressable, ScrollView, Text, TextInput, View } from 'react-native';
import '../global.css';
import { styles } from '@/styles/styles';
import { Role } from '@/types/entityTypes';
import { useCreateUser } from '@/hooks/useUsers';
import { checkUsernameAvailability } from '@/api/auth';
import { Heading } from '@/components/ui/heading';
import {
  getUsernameValidationError,
  normalizeUsername,
} from '@/utils/username';
import { getEmailValidationError, normalizeEmail } from '@/utils/email';

type SignUpForm = {
  firstName: string;
  lastName: string;
  username: string;
  email: string;
  phoneNumber: string;
  organizationName: string;
  role: Role;
  password: string;
  confirmPassword: string;
};

export default function SignUpPage() {
  const createUser = useCreateUser();

  const {
    control,
    handleSubmit,
    getValues,
    watch,
    setValue,
    clearErrors,
    formState: { errors, isSubmitting },
  } = useForm<SignUpForm>({
    mode: 'onBlur',
    defaultValues: {
      firstName: '',
      lastName: '',
      username: '',
      email: '',
      phoneNumber: '',
      organizationName: '',
      role: Role.CLEANER,
      password: '',
      confirmPassword: '',
    },
  });

  const selectedRole = watch('role');

  const handleSignUp = async (form: SignUpForm) => {
    const {
      confirmPassword: _confirmPassword,
      organizationName,
      ...accountData
    } = form;

    const registrationPayload = {
      ...accountData,
      firstName: accountData.firstName.trim(),
      lastName: accountData.lastName.trim(),
      username: accountData.username.trim(),
      email: normalizeEmail(accountData.email),
      phoneNumber: accountData.phoneNumber.trim(),
      ...(accountData.role === Role.MANAGER
        ? { organizationName: organizationName.trim() }
        : {}),
    };

    await createUser.mutateAsync(registrationPayload);
  };

  return (
    <ScrollView
      style={styles.modalScreen}
      contentContainerStyle={styles.modalScreenContent}
      keyboardShouldPersistTaps='handled'
    >
      {/* Header */}
      <Text style={styles.modalHeader}>
        <Heading size='2xl'>Create Account</Heading>
      </Text>

      {/* Content */}
      <View style={styles.formCard}>
        <Text>First Name</Text>
        <Controller
          control={control}
          name='firstName'
          rules={{ required: 'First name is required' }}
          render={({ field: { value, onChange, onBlur } }) => (
            <TextInput
              style={[styles.input, errors.firstName && styles.inputError]}
              placeholder='Enter your first name'
              value={value}
              onChangeText={onChange}
              onBlur={onBlur}
              autoCapitalize='words'
              textContentType='givenName'
            />
          )}
        />
        {errors.firstName && (
          <Text style={styles.errorText}>{errors.firstName.message}</Text>
        )}

        <Text>Last Name</Text>
        <Controller
          control={control}
          name='lastName'
          rules={{ required: 'Last name is required' }}
          render={({ field: { value, onChange, onBlur } }) => (
            <TextInput
              style={[styles.input, errors.lastName && styles.inputError]}
              placeholder='Enter your last name'
              value={value}
              onChangeText={onChange}
              onBlur={onBlur}
              autoCapitalize='words'
              textContentType='familyName'
            />
          )}
        />
        {errors.lastName && (
          <Text style={styles.errorText}>{errors.lastName.message}</Text>
        )}

        <Text>Username</Text>
        <Controller
          control={control}
          name='username'
          rules={{
            validate: async (value) => {
              const validationError = getUsernameValidationError(value);
              if (validationError) {
                return validationError;
              }

              const username = normalizeUsername(value);
              try {
                const result = await checkUsernameAvailability(username);

                return result.available || 'Username is already taken';
              } catch {
                return 'Unable to check username availability';
              }
            },
          }}
          render={({ field: { value, onChange, onBlur } }) => (
            <TextInput
              style={[styles.input, errors.username && styles.inputError]}
              placeholder='Choose a username'
              value={value}
              onChangeText={onChange}
              onBlur={onBlur}
              autoCapitalize='none'
              autoCorrect={false}
              textContentType='username'
            />
          )}
        />
        {errors.username && (
          <Text style={styles.errorText}>{errors.username.message}</Text>
        )}

        <Text>Email</Text>
        <Controller
          control={control}
          name='email'
          rules={{
            required: 'Email is required',
            validate: (value) => getEmailValidationError(value) ?? true,
          }}
          render={({ field: { value, onChange, onBlur } }) => (
            <TextInput
              style={[styles.input, errors.email && styles.inputError]}
              placeholder='Enter your email'
              value={value}
              onChangeText={onChange}
              onBlur={onBlur}
              autoCapitalize='none'
              autoCorrect={false}
              keyboardType='email-address'
              textContentType='emailAddress'
            />
          )}
        />
        {errors.email && (
          <Text style={styles.errorText}>{errors.email.message}</Text>
        )}

        <Text>Phone Number</Text>
        <Controller
          control={control}
          name='phoneNumber'
          rules={{
            required: 'Phone number is required',
            pattern: {
              value: /^[+\d][\d\s().-]{6,}$/,
              message: 'Enter a valid phone number',
            },
          }}
          render={({ field: { value, onChange, onBlur } }) => (
            <TextInput
              style={[styles.input, errors.phoneNumber && styles.inputError]}
              placeholder='Enter your phone number'
              value={value}
              onChangeText={onChange}
              onBlur={onBlur}
              keyboardType='phone-pad'
              textContentType='telephoneNumber'
            />
          )}
        />
        {errors.phoneNumber && (
          <Text style={styles.errorText}>{errors.phoneNumber.message}</Text>
        )}

        <Text>Account Type</Text>
        <Controller
          control={control}
          name='role'
          render={({ field: { value, onChange } }) => (
            <View style={styles.roleRow}>
              <Pressable
                accessibilityRole='radio'
                accessibilityState={{ checked: value === Role.CLEANER }}
                style={[
                  styles.roleButton,
                  value === Role.CLEANER && styles.button,
                ]}
                onPress={() => {
                  onChange(Role.CLEANER);
                  setValue('organizationName', '');
                  clearErrors('organizationName');
                }}
              >
                <Text style={styles.buttonText}>Cleaner</Text>
              </Pressable>

              <Pressable
                accessibilityRole='radio'
                accessibilityState={{ checked: value === Role.MANAGER }}
                style={[
                  styles.roleButton,
                  value === Role.MANAGER && styles.button,
                ]}
                onPress={() => onChange(Role.MANAGER)}
              >
                <Text style={styles.buttonText}>Manager</Text>
              </Pressable>
            </View>
          )}
        />

        {selectedRole === Role.CLEANER ? (
          <Text className='text-sm text-gray-500'>
            A manager can invite you to their organization after signup.
          </Text>
        ) : (
          <>
            <Text>Organization</Text>
            <Controller
              control={control}
              name='organizationName'
              rules={{ required: 'Organization name is required' }}
              render={({ field: { value, onChange, onBlur } }) => (
                <TextInput
                  style={[
                    styles.input,
                    errors.organizationName && styles.inputError,
                  ]}
                  placeholder='Enter your organization name'
                  value={value}
                  onChangeText={onChange}
                  onBlur={onBlur}
                  autoCapitalize='words'
                />
              )}
            />
            {errors.organizationName && (
              <Text style={styles.errorText}>
                {errors.organizationName.message}
              </Text>
            )}
          </>
        )}

        <Text>Password</Text>
        <Controller
          control={control}
          name='password'
          rules={{
            required: 'Password is required',
            minLength: {
              value: 8,
              message: 'Password must be at least 8 characters',
            },
          }}
          render={({ field: { value, onChange, onBlur } }) => (
            <TextInput
              style={[styles.input, errors.password && styles.inputError]}
              placeholder='Enter at least 8 characters'
              value={value}
              onChangeText={onChange}
              onBlur={onBlur}
              autoCapitalize='none'
              secureTextEntry
              textContentType='newPassword'
            />
          )}
        />
        {errors.password && (
          <Text style={styles.errorText}>{errors.password.message}</Text>
        )}

        <Text>Confirm Password</Text>
        <Controller
          control={control}
          name='confirmPassword'
          rules={{
            required: 'Please confirm your password',
            validate: (value) =>
              value === getValues('password') || 'Passwords do not match',
          }}
          render={({ field: { value, onChange, onBlur } }) => (
            <TextInput
              style={[
                styles.input,
                errors.confirmPassword && styles.inputError,
              ]}
              placeholder='Enter your password again'
              value={value}
              onChangeText={onChange}
              onBlur={onBlur}
              autoCapitalize='none'
              secureTextEntry
              textContentType='newPassword'
              returnKeyType='done'
              onSubmitEditing={() => void handleSubmit(handleSignUp)()}
            />
          )}
        />
        {errors.confirmPassword && (
          <Text style={styles.errorText}>{errors.confirmPassword.message}</Text>
        )}

        <Pressable
          style={[styles.button, isSubmitting && styles.disabledButton]}
          className='mt-2'
          onPress={() => void handleSubmit(handleSignUp)()}
          disabled={isSubmitting}
        >
          <Text style={styles.buttonText}>
            {isSubmitting ? 'Creating Account...' : 'Create Account'}
          </Text>
        </Pressable>
      </View>
    </ScrollView>
  );
}
