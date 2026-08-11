import React, { useState } from 'react';
import { Alert, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { ContractionSessionResponse } from '../types/api';
import { useEndContractionSession, useRecordContraction, useStartContractionSession } from '../hooks/useContractionTimer';

export function ContractionTimerScreen() {
  const [session, setSession] = useState<ContractionSessionResponse | null>(null);
  const { mutate: start, isPending: starting } = useStartContractionSession();
  const { mutate: record, isPending: recording } = useRecordContraction(session?.id ?? '');
  const { mutate: end, isPending: ending } = useEndContractionSession(session?.id ?? '');

  const handleEnd = () =>
    end(undefined, {
      onSuccess: data => {
        const msg = data.isLaborPattern
          ? `Avg interval: ${data.averageIntervalSeconds}s\nAvg duration: ${data.averageDurationSeconds}s`
          : `${data.totalContractions} contractions recorded`;
        Alert.alert(
          data.alertTriggered ? '⚠️ Possible premature labor' : data.isLaborPattern ? 'Labor pattern detected' : 'Session complete',
          msg,
        );
        setSession(null);
      },
    });

  return (
    <View style={styles.container}>
      {!session ? (
        <TouchableOpacity style={styles.bigButton} onPress={() => start(undefined, { onSuccess: setSession })} disabled={starting}>
          <Text style={styles.bigButtonText}>Start</Text>
        </TouchableOpacity>
      ) : (
        <>
          <Text style={styles.count}>{session.totalContractions}</Text>
          <Text style={styles.label}>contractions</Text>
          {session.averageIntervalSeconds != null && (
            <Text style={styles.stat}>Avg interval: {Math.round(session.averageIntervalSeconds / 60)} min</Text>
          )}
          <TouchableOpacity style={styles.recordButton} onPress={() => record(undefined, { onSuccess: setSession })} disabled={recording}>
            <Text style={styles.recordText}>Record Contraction</Text>
          </TouchableOpacity>
          <TouchableOpacity style={styles.endButton} onPress={handleEnd} disabled={ending}>
            <Text style={styles.endText}>End Session</Text>
          </TouchableOpacity>
        </>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#fff', justifyContent: 'center', alignItems: 'center', padding: 24 },
  bigButton: { backgroundColor: '#E91E8C', borderRadius: 80, width: 160, height: 160, justifyContent: 'center', alignItems: 'center' },
  bigButtonText: { color: '#fff', fontSize: 20, fontWeight: '700' },
  count: { fontSize: 72, fontWeight: '700', color: '#E91E8C' },
  label: { fontSize: 18, color: '#666', marginBottom: 8 },
  stat: { fontSize: 14, color: '#444', marginBottom: 32 },
  recordButton: { backgroundColor: '#E91E8C', borderRadius: 8, paddingHorizontal: 32, paddingVertical: 16, marginBottom: 16 },
  recordText: { color: '#fff', fontWeight: '700', fontSize: 16 },
  endButton: { borderWidth: 1, borderColor: '#E91E8C', borderRadius: 8, paddingHorizontal: 32, paddingVertical: 12 },
  endText: { color: '#E91E8C', fontWeight: '600', fontSize: 16 },
});
