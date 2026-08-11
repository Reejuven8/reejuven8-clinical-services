import React, { useState } from 'react';
import { Alert, ScrollView, StyleSheet, Text, TextInput, TouchableOpacity, View } from 'react-native';
import { useLogSymptom } from '../hooks/useSymptomLog';

const COMMON_SYMPTOMS = [
  'Headache', 'Nausea', 'Swelling', 'Back pain',
  'Fatigue', 'Reduced fetal movement', 'Blurred vision', 'Contractions',
];

export function SymptomLogScreen() {
  const [selected, setSelected] = useState<string[]>([]);
  const [custom, setCustom] = useState('');
  const { mutate: logSymptom, isPending } = useLogSymptom();

  const toggle = (s: string) =>
    setSelected(prev => prev.includes(s) ? prev.filter(x => x !== s) : [...prev, s]);

  const handleSubmit = () => {
    const symptoms = custom.trim() ? [...selected, custom.trim()] : selected;
    if (!symptoms.length) { Alert.alert('Select at least one symptom'); return; }
    logSymptom(
      { symptoms },
      {
        onSuccess: data => Alert.alert(
          data.severityFlag === 'CRITICAL' ? '⚠️ Critical' : data.severityFlag === 'WARNING' ? 'Warning' : 'Logged',
          data.triageResult.join('\n') || 'Symptoms recorded.',
        ),
        onError: e => Alert.alert('Error', (e as Error).message),
      },
    );
  };

  return (
    <ScrollView style={styles.container}>
      <Text style={styles.label}>Select symptoms</Text>
      <View style={styles.chips}>
        {COMMON_SYMPTOMS.map(s => (
          <TouchableOpacity
            key={s}
            style={[styles.chip, selected.includes(s) && styles.chipActive]}
            onPress={() => toggle(s)}
          >
            <Text style={[styles.chipText, selected.includes(s) && styles.chipTextActive]}>{s}</Text>
          </TouchableOpacity>
        ))}
      </View>
      <TextInput
        style={styles.input}
        placeholder="Other symptom…"
        value={custom}
        onChangeText={setCustom}
      />
      <TouchableOpacity style={styles.button} onPress={handleSubmit} disabled={isPending}>
        <Text style={styles.buttonText}>{isPending ? 'Logging…' : 'Log Symptoms'}</Text>
      </TouchableOpacity>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#fff', padding: 16 },
  label: { fontSize: 16, fontWeight: '600', color: '#333', marginBottom: 12 },
  chips: { flexDirection: 'row', flexWrap: 'wrap', gap: 8, marginBottom: 16 },
  chip: { borderWidth: 1, borderColor: '#ddd', borderRadius: 20, paddingHorizontal: 14, paddingVertical: 8 },
  chipActive: { backgroundColor: '#E91E8C', borderColor: '#E91E8C' },
  chipText: { fontSize: 14, color: '#333' },
  chipTextActive: { color: '#fff' },
  input: { borderWidth: 1, borderColor: '#ddd', borderRadius: 8, padding: 12, marginBottom: 16, fontSize: 15 },
  button: { backgroundColor: '#E91E8C', borderRadius: 8, padding: 16, alignItems: 'center' },
  buttonText: { color: '#fff', fontWeight: '600', fontSize: 16 },
});
