import React, { useState } from 'react';
import { Alert, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { KickCounterSessionResponse } from '../types/api';
import { useEndKickSession, useRecordKick, useStartKickSession } from '../hooks/useKickCounter';

export function KickCounterScreen() {
  const [session, setSession] = useState<KickCounterSessionResponse | null>(null);
  const { mutate: start, isPending: starting } = useStartKickSession();
  const { mutate: kick, isPending: kicking } = useRecordKick(session?.id ?? '');
  const { mutate: end, isPending: ending } = useEndKickSession(session?.id ?? '');

  const handleEnd = () =>
    end(undefined, {
      onSuccess: data => {
        Alert.alert(
          data.isConcerning ? '⚠️ Contact your doctor' : 'Session complete',
          `${data.totalKicks} kicks recorded`,
        );
        setSession(null);
      },
    });

  return (
    <View style={styles.container}>
      {!session ? (
        <TouchableOpacity style={styles.bigButton} onPress={() => start(undefined, { onSuccess: setSession })} disabled={starting}>
          <Text style={styles.bigButtonText}>Start Session</Text>
        </TouchableOpacity>
      ) : (
        <>
          <Text style={styles.count}>{session.totalKicks}</Text>
          <Text style={styles.label}>kicks</Text>
          <TouchableOpacity style={styles.kickButton} onPress={() => kick(undefined, { onSuccess: setSession })} disabled={kicking}>
            <Text style={styles.kickButtonText}>+ Kick</Text>
          </TouchableOpacity>
          <TouchableOpacity style={styles.endButton} onPress={handleEnd} disabled={ending}>
            <Text style={styles.endButtonText}>End Session</Text>
          </TouchableOpacity>
        </>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#fff', justifyContent: 'center', alignItems: 'center', padding: 24 },
  bigButton: { backgroundColor: '#E91E8C', borderRadius: 80, width: 160, height: 160, justifyContent: 'center', alignItems: 'center' },
  bigButtonText: { color: '#fff', fontSize: 18, fontWeight: '700' },
  count: { fontSize: 80, fontWeight: '700', color: '#E91E8C' },
  label: { fontSize: 20, color: '#666', marginBottom: 32 },
  kickButton: { backgroundColor: '#E91E8C', borderRadius: 60, width: 120, height: 120, justifyContent: 'center', alignItems: 'center', marginBottom: 24 },
  kickButtonText: { color: '#fff', fontSize: 22, fontWeight: '700' },
  endButton: { borderWidth: 1, borderColor: '#E91E8C', borderRadius: 8, paddingHorizontal: 32, paddingVertical: 12 },
  endButtonText: { color: '#E91E8C', fontWeight: '600', fontSize: 16 },
});
