import React from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { useCurrentWeekTimeline } from '../hooks/useTimeline';
import { LoadingSpinner } from '../components/LoadingSpinner';
import { ErrorView } from '../components/ErrorView';

export function TimelineScreen() {
  const { data, isLoading, error, refetch } = useCurrentWeekTimeline();

  if (isLoading) return <LoadingSpinner />;
  if (error) return <ErrorView error={error as Error} onRetry={refetch} />;

  return (
    <ScrollView style={styles.container}>
      <Text style={styles.week}>Week {data?.gestationalWeek}</Text>
      <Text style={styles.trimester}>Trimester {data?.trimester}</Text>
      <Text style={styles.sectionTitle}>Baby this week</Text>
      <Text style={styles.body}>{data?.babyDevelopment.sizeComparison}</Text>
      {data?.babyDevelopment.developmentHighlights.map((h, i) => (
        <Text key={i} style={styles.bullet}>• {h}</Text>
      ))}
      <Text style={styles.sectionTitle}>Maternal changes</Text>
      {data?.maternalChanges.map((c, i) => (
        <Text key={i} style={styles.bullet}>• {c}</Text>
      ))}
      <Text style={styles.sectionTitle}>Milestones</Text>
      {data?.scheduledMilestones.map((m, i) => (
        <View key={i} style={styles.milestoneCard}>
          <Text style={styles.milestoneTitle}>{m.title}</Text>
          <Text style={styles.body}>{m.description}</Text>
        </View>
      ))}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#fff', padding: 16 },
  week: { fontSize: 32, fontWeight: '700', color: '#E91E8C' },
  trimester: { fontSize: 14, color: '#666', marginBottom: 24 },
  sectionTitle: { fontSize: 18, fontWeight: '600', color: '#333', marginTop: 20, marginBottom: 8 },
  body: { fontSize: 15, color: '#444', lineHeight: 22 },
  bullet: { fontSize: 15, color: '#444', lineHeight: 22 },
  milestoneCard: { backgroundColor: '#FFF0F7', borderRadius: 8, padding: 12, marginBottom: 8 },
  milestoneTitle: { fontWeight: '600', color: '#E91E8C', marginBottom: 4 },
});
