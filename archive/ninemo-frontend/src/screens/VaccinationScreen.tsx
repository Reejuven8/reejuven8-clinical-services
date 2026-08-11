import React from 'react';
import { Alert, ScrollView, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { useSelector } from 'react-redux';
import { RootState } from '../store';
import { useMarkVaccineCompleted, useVaccinationSchedule } from '../hooks/useVaccination';
import { LoadingSpinner } from '../components/LoadingSpinner';
import { ErrorView } from '../components/ErrorView';

const STATUS_COLORS: Record<string, string> = {
  COMPLETED: '#2E7D32',
  PENDING: '#1565C0',
  OVERDUE: '#B71C1C',
  SKIPPED: '#757575',
};

export function VaccinationScreen() {
  const childId = useSelector((state: RootState) => state.ui.activeChildId);
  const { data, isLoading, error, refetch } = useVaccinationSchedule(childId ?? '');
  const { mutate: markCompleted } = useMarkVaccineCompleted(childId ?? '');

  const handleMark = (id: string, name: string) => {
    Alert.alert(`Mark ${name} as done?`, '', [
      { text: 'Cancel', style: 'cancel' },
      {
        text: 'Confirm',
        onPress: () => markCompleted({ id, date: new Date().toISOString().split('T')[0], by: 'Doctor' }),
      },
    ]);
  };

  if (!childId) {
    return <View style={styles.container}><Text style={styles.empty}>No child profile selected.</Text></View>;
  }
  if (isLoading) return <LoadingSpinner />;
  if (error) return <ErrorView error={error as Error} onRetry={refetch} />;

  return (
    <ScrollView style={styles.container}>
      {data?.map(v => (
        <View key={v.id} style={styles.row}>
          <View style={styles.info}>
            <Text style={styles.name}>{v.vaccineName} (dose {v.doseNumber})</Text>
            <Text style={styles.date}>Due: {new Date(v.scheduledDate).toLocaleDateString()}</Text>
          </View>
          <TouchableOpacity
            style={[styles.badge, { backgroundColor: STATUS_COLORS[v.status] ?? '#999' }]}
            onPress={() =>
              (v.status === 'PENDING' || v.status === 'OVERDUE') ? handleMark(v.id, v.vaccineName) : null
            }
          >
            <Text style={styles.badgeText}>{v.status}</Text>
          </TouchableOpacity>
        </View>
      ))}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#fff', padding: 16 },
  empty: { color: '#666', fontSize: 15, textAlign: 'center', marginTop: 40 },
  row: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingVertical: 12, borderBottomWidth: 1, borderColor: '#eee' },
  info: { flex: 1 },
  name: { fontWeight: '600', color: '#333', fontSize: 15 },
  date: { fontSize: 12, color: '#666', marginTop: 2 },
  badge: { borderRadius: 4, paddingHorizontal: 8, paddingVertical: 4 },
  badgeText: { color: '#fff', fontSize: 11, fontWeight: '700' },
});
