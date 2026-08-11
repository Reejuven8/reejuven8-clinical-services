import React, { useState } from 'react';
import { Alert, ScrollView, StyleSheet, Text, TextInput, TouchableOpacity, View } from 'react-native';
import { useLogVitals, useVitalsByType } from '../hooks/useVitals';
import { LoadingSpinner } from '../components/LoadingSpinner';

export function VitalsWeightScreen() {
  const [weight, setWeight] = useState('');
  const { data: history, isLoading } = useVitalsByType('WEIGHT');
  const { mutate: logVital, isPending } = useLogVitals();

  const handleLog = () => {
    const kg = parseFloat(weight);
    if (isNaN(kg)) { Alert.alert('Enter a valid weight'); return; }
    logVital(
      { vitalType: 'WEIGHT', measurements: { weightKg: kg } },
      {
        onSuccess: () => { Alert.alert('Weight logged'); setWeight(''); },
        onError: e => Alert.alert('Error', (e as Error).message),
      },
    );
  };

  if (isLoading) return <LoadingSpinner />;

  return (
    <ScrollView style={styles.container}>
      <TextInput
        style={styles.input}
        placeholder="Weight in kg"
        keyboardType="decimal-pad"
        value={weight}
        onChangeText={setWeight}
      />
      <TouchableOpacity style={styles.button} onPress={handleLog} disabled={isPending}>
        <Text style={styles.buttonText}>{isPending ? 'Saving…' : 'Log Weight'}</Text>
      </TouchableOpacity>
      {history?.map((v, i) => (
        <View key={i} style={styles.row}>
          <Text style={styles.rowDate}>{new Date(v.loggedAt).toLocaleDateString()}</Text>
          <Text style={styles.rowValue}>{v.measurements.weightKg} kg</Text>
          {v.alertTriggered && <Text style={styles.alert}>⚠️</Text>}
        </View>
      ))}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#fff', padding: 16 },
  input: { borderWidth: 1, borderColor: '#ddd', borderRadius: 8, padding: 12, marginBottom: 12, fontSize: 16 },
  button: { backgroundColor: '#E91E8C', borderRadius: 8, padding: 16, alignItems: 'center', marginBottom: 24 },
  buttonText: { color: '#fff', fontWeight: '600', fontSize: 16 },
  row: { flexDirection: 'row', justifyContent: 'space-between', paddingVertical: 10, borderBottomWidth: 1, borderColor: '#eee' },
  rowDate: { color: '#666', fontSize: 14 },
  rowValue: { fontWeight: '600', color: '#333' },
  alert: { fontSize: 14 },
});
