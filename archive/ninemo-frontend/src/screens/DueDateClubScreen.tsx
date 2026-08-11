import React from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { useMyClubs } from '../hooks/useCommunity';
import { LoadingSpinner } from '../components/LoadingSpinner';
import { ErrorView } from '../components/ErrorView';

export function DueDateClubScreen() {
  const { data: clubs, isLoading, error, refetch } = useMyClubs();

  if (isLoading) return <LoadingSpinner />;
  if (error) return <ErrorView error={error as Error} onRetry={refetch} />;

  return (
    <ScrollView style={styles.container}>
      {clubs?.length === 0 && <Text style={styles.empty}>You haven't joined a club yet.</Text>}
      {clubs?.map(club => (
        <View key={club.id} style={styles.card}>
          <Text style={styles.clubName}>{club.clubName}</Text>
          <Text style={styles.members}>{club.memberCount} members · {club.dueDateMonth}</Text>
          {club.channels.map(ch => (
            <Text key={ch.channelId} style={styles.channel}>#{ch.name}</Text>
          ))}
        </View>
      ))}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#fff', padding: 16 },
  empty: { color: '#666', fontSize: 15, textAlign: 'center', marginTop: 40 },
  card: { backgroundColor: '#FFF0F7', borderRadius: 8, padding: 14, marginBottom: 12 },
  clubName: { fontWeight: '700', fontSize: 16, color: '#333', marginBottom: 4 },
  members: { fontSize: 13, color: '#666', marginBottom: 8 },
  channel: { fontSize: 13, color: '#E91E8C' },
});
