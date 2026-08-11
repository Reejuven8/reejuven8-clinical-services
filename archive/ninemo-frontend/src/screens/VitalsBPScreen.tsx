import React, { useState } from 'react';
import { Alert, ScrollView, StyleSheet, Text, TextInput, TouchableOpacity, View } from 'react-native';
import { useLogVitals, useVitalsByType } from '../hooks/useVitals';
import { LoadingSpinner } from '../components/LoadingSpinner';

export function VitalsBPScreen() {
  const [systolic, setSystolic] = useState('');
  const [diastolic, setDiastolic] = useState('');
  const { data: history, isLoading } = useVitalsByType('BLOOD_PRESSURE');
  const { mutate: logVital, isPending } = useLogVitals();

  const handleLog = () => {
    const s = parseInt(systolic, 10);
    const d = parseInt(diastolic, 10);
    if (isNaN(s) || isNaN(d)) { Alert.alert('Enter valid BP values'); return; }
    logVital(
      { vitalType: 'BLOOD_PRESSURE', measurements: { bpSystolic: s, bpDiastolic: d } },
      {
        onSuccess: () => { Alert.alert('BP logged'); setSystolic(''); setDiastolic(''); },
        onError: e => Alert.alert('Error', (e as Error).message),
      },
    );
  };

  if (isLoading) return <LoadingSpinner />;

  return (
    <ScrollView style={styles.container}>
      <View style={styles.row}>
        <TextInput style={[styles.input, { flex: 1, marginRight: 8 }]} placeholder="Systolic" keyboardType="number-pad" value={systolic} onChangeText={setSystolic} />
        <TextInput style={[styles.input, { flex: 1 }]} placeholder="Diastolic" keyboardType="number-pad" value={diastolic} onChangeText={setDiastolic} />
      </View>
      <TouchableOpacity style={styles.button} onPress={handleLog} disabled={isPending}>
        <Text style={styles.buttonText}>{isPending ? 'Saving…' : 'Log Blood Pressure'}</Text>
      </TouchableOpacity>
      {history?.map((v, i) => (
        <View key={i} style={styles.historyRow}>
          <Text style={styles.date}>{new Date(v.loggedAt).toLocaleDateString()}</Text>
          <Text style={styles.value}>{v.measurements.bpSystolic}/{v.measurements.bpDiastolic} mmHg</Text>
          {v.alertTriggered && <Text style={styles.alert}>⚠️</Text>}
        </View>
      ))}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#fff', padding: 16 },
  row: { flexDirection: 'row', marginBottom: 12 },
  input: { borderWidth: 1, borderColor: '#ddd', borderRadius: 8, padding: 12, fontSize: 16 },
  button: { backgroundColor: '#E91E8C', borderRadius: 8, padding: 16, alignItems: 'center', marginBottom: 24 },
  buttonText: { color: '#fff', fontWeight: '600', fontSize: 16 },
  historyRow: { flexDirection: 'row', justifyContent: 'space-between', paddingVertical: 10, borderBottomWidth: 1, borderColor: '#eee' },
  date: { color: '#666', fontSize: 14 },
  value: { fontWeight: '600', color: '#333' },
  alert: { fontSize: 14 },
});
