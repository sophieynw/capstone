import React from 'react';
import { View, type ViewProps } from 'react-native';

import { Spinner } from '@/components/ui/spinner';
import { Text } from '@/components/ui/text';

type LoadingSpinnerProps = ViewProps & {
  label?: string;
  inline?: boolean;
  spinnerSize?: React.ComponentProps<typeof Spinner>['size'];
  spinnerColor?: React.ComponentProps<typeof Spinner>['color'];
  className?: string;
};

export function LoadingSpinner({
  label,
  inline = false,
  spinnerSize = 'large',
  spinnerColor = '#171717',
  className = '',
  ...viewProps
}: LoadingSpinnerProps) {
  const containerClassName = inline
    ? 'min-h-24 w-full items-center justify-center gap-3 py-4'
    : 'flex-1 items-center justify-center gap-3 bg-background';

  return (
    <View
      accessibilityLiveRegion='polite'
      {...viewProps}
      className={`${containerClassName} ${className}`.trim()}
    >
      <Spinner
        size={spinnerSize}
        color={spinnerColor}
        aria-label={label ?? 'Loading'}
      />
      {label ? <Text className='text-muted-foreground'>{label}</Text> : null}
    </View>
  );
}
