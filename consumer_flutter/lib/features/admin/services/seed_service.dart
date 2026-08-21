import '../../../core/network/api_client.dart';

/// Development/admin seed action backed by the real Spring Boot endpoint.
/// The backend owns idempotency and the seed data; Flutter no longer writes
/// directly to Firebase.
class SeedService {
  final ApiClient _client;

  SeedService({ApiClient? client}) : _client = client ?? ApiClient.instance;

  Future<String> seedAll() async {
    final data = await _client.post('/admin/seed');
    return data?.toString() ?? 'Seed request completed';
  }

  /// The REST backend does not expose a client-side "already seeded" query.
  /// Calling seedAll() is safe because AdminService performs the idempotency
  /// check on the server.
  Future<bool> isAlreadySeeded() async => false;
}
