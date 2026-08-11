import React from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { useSelector } from 'react-redux';
import { RootState } from '../store';
import { useGrowthHistory } from '../hooks/useGrowthChart';
import { LoadingSpinner } from '../components/LoadingSpinner';
import { ErrorView } from '../components/ErrorView';

export function GrowthChartScreen() {
  const childId = useSelector((state: RootState) => state.ui.activeChildId);
  const { data, isLoading, error, refetch } = useGrowthHistory(childId ?? '');

  if (!childId) {
    return <View style={styles.container}><Text style={styles.empty}>No child profile selected.</Text></View>;
  }
  if (isLoading) return <LoadingSpinner />;
  if (error) return <ErrorView error={error as Error} onRetry={refetch} />;

  return (
    <ScrollView style={styles.container}>
      {data?.map((m, i) => (
        <View key={i} style={styles.card}>
          <Text style={styles.age}>Month {m.ageInMonths}</Text>
          <Text style={styles.stat}>Weight: {m.weightKg} kg · Height: {m.heightCm} cm</Text>
          {m.headCircumferenceCm != null && <Text style={styles.stat}>Head: {m.headCircumferenceCm} cm</Text>}
          {m.alertFlags.length > 0 && <Text style={styles.alert}>⚠️ {m.alertFlags.join(', ')}</Text>}
        </View>
      ))}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#fff', padding: 16 },
  empty: { color: '#666', fontSize: 15, textAlign: 'center', marginTop: 40 },
  card: { backgroundColor: '#F3E5F5', borderRadius: 8, padding: 12, marginBottom: 12 },
  age: { fontWeight: '700', color: '#6A1B9A', marginBottom: 4 },
  stat: { fontSize: 14, color: '#333' },
  alert: { color: '#B71C1C', fontSize: 13, marginTop: 4 },
});
