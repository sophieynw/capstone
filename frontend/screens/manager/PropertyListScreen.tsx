import { Pressable, ScrollView, Text, View } from 'react-native';
import { Card } from '@/components/ui/card';
import { Button, ButtonIcon, ButtonText } from '@/components/ui/button';
import { usePropertyAll } from '@/hooks/useProperties';
import { Property } from '@/types/entityTypes';
import { useNextCleaningByProperty } from '@/hooks/useCleanings';
import { toFriendlyDate } from '@/utils/helpers';
import { styles } from '@/styles/styles';
import { Heading } from '@/components/ui/heading';
import { HousePlus } from 'lucide-react-native';
import { LoadingSpinner } from '@/components/LoadingSpinner';
import { ErrorState } from '@/components/ErrorState';

// region Property Card

type PropertyCardProps = {
  readonly property: Property;
  navigation: any;
};

function PropertyCard({ property, navigation }: PropertyCardProps) {
  const { data: cleaning } = useNextCleaningByProperty(property?.id);

  return (
    <Pressable
      onPress={() => {
        navigation.navigate('PropertyDetailsScreen', {
          propertyId: property.id,
        });
      }}
    >
      <Card style={styles.mediumCard}>
        <Heading size='md'>{property?.name}</Heading>

        <View className='gap-1'>
          <Text>
            {property?.unit ? `${property.unit}-` : ''}
            {property?.street}, {property?.city}
          </Text>

          <Text>
            Next Cleaning:{' '}
            {cleaning?.dateTimeStart
              ? toFriendlyDate(cleaning.dateTimeStart)
              : 'None scheduled'}
          </Text>
        </View>
      </Card>
    </Pressable>
  );
}

// endregion Property Card

export default function PropertyListScreen({ navigation }: any) {
  const { data: properties, isLoading, isError, error } = usePropertyAll();

  if (isLoading) {
    return <LoadingSpinner label='Loading properties...' />;
  }

  if (isError) {
    console.error(error);
    return <ErrorState />;
  }

  return (
    <ScrollView
      style={styles.screen}
      contentContainerStyle={styles.screenContent}
    >
      <View style={styles.vStack}>
        {/* Properties */}
        {properties?.map((property) => (
          <PropertyCard
            key={property.id}
            property={property}
            navigation={navigation}
          />
        ))}

        {/* Add a property */}
        <Button
          className='w-full'
          style={styles.button}
          size='lg'

          onPress={() => navigation.navigate('NewPropertyScreen')}
        >
          <ButtonIcon style={styles.buttonText} as={HousePlus} />
          <ButtonText style={styles.buttonText}>Add a property</ButtonText>
        </Button>
      </View>
    </ScrollView>
  );
}
