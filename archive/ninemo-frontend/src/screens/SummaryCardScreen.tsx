import React from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { useSummaryCard } from '../hooks/useSummaryCard';
import { LoadingSpinner } from '../components/LoadingSpinner';
import { ErrorView } from '../components/ErrorView';

export function SummaryCardScreen() {
  const { data, isLoading, error, refetch } = useSummaryCard();

  if (isLoading) return <LoadingSpinner />;
  if (error) return <ErrorView error={error as Error} onRetry={refetch} />;

  return (
    <ScrollView style={styles.container}>
      <Text style={styles.week}>Week {data?.gestationalWeek} — T{data?.trimester}</Text>
      <Text style={styles.edd}>EDD: {data?.edd ? new Date(data.edd).toLocaleDateString() : '—'}</Text>
      {data?.highRiskFlags.length ? (
        <View style={styles.alertBox}>
          <Text style={styles.alertTitle}>⚠️ Risk Flags</Text>
          {data.highRiskFlags.map((f, i) => <Text key={i} style={styles.alertItem}>• {f}</Text>)}
        </View>
      ) : null}
      <Text style={styles.sectionTitle}>Latest Vitals</Text>
      {data?.latestVitals.map((v, i) => (
        <View key={i} style={styles.row}>
          <Text style={styles.rowLabel}>{v.vitalType}</Text>
          <Text style={styles.rowValue}>{JSON.stringify(v.measurements)}</Text>
        </View>
      ))}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#fff', padding: 16 },
  week: { fontSize: 24, fontWeight: '700', color: '#E91E8C', marginBottom: 4 },
  edd: { fontSize: 14, color: '#666', marginBottom: 20 },
  alertBox: { backgroundColor: '#FFF3E0', borderRadius: 8, padding: 12, marginBottom: 16 },
  alertTitle: { fontWeight: '700', color: '#E65100', marginBottom: 8 },
  alertItem: { color: '#BF360C', fontSize: 14 },
  sectionTitle: { fontSize: 16, fontWeight: '600', color: '#333', marginBottom: 8, marginTop: 8 },
  row: { flexDirection: 'row', justifyContent: 'space-between', paddingVertical: 8, borderBottomWidth: 1, borderColor: '#eee' },
  rowLabel: { color: '#666', fontSize: 14 },
  rowValue: { color: '#333', fontWeight: '500', fontSize: 14 },
});
