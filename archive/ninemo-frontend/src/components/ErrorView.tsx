import React from 'react';
import { StyleSheet, Text, TouchableOpacity, View } from 'react-native';

interface Props {
  error: Error;
  onRetry?: () => void;
}

export function ErrorView({ error, onRetry }: Props) {
  return (
    <View style={styles.container}>
      <Text style={styles.message}>{error.message}</Text>
      {onRetry && (
        <TouchableOpacity style={styles.button} onPress={onRetry}>
          <Text style={styles.buttonText}>Retry</Text>
        </TouchableOpacity>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, justifyContent: 'center', alignItems: 'center', padding: 24 },
  message: { fontSize: 16, color: '#B00020', textAlign: 'center', marginBottom: 16 },
  button: { backgroundColor: '#E91E8C', borderRadius: 8, paddingHorizontal: 24, paddingVertical: 12 },
  buttonText: { color: '#fff', fontWeight: '600' },
});
