package org.jboss.as.console.client.shared.runtime.jms;

/**
 * @author Heiko Braun
 * @date 02/07/14
 */
public class MessagingResources {

    public static final String queueDescription =
            "bwAAAAUAC2Rlc2NyaXB0aW9ucwAURGVmaW5lcyBhIEpNUyBxdWV1ZS4AEmFjY2Vzcy1jb25zdHJh\n" +
            "aW50c28AAAABAAthcHBsaWNhdGlvbm8AAAABAAlqbXMtcXVldWVvAAAAAQAEdHlwZXMACW1lc3Nh\n" +
            "Z2luZwAKYXR0cmlidXRlc28AAAANAAhzZWxlY3Rvcm8AAAAIAAR0eXBldHMAC2Rlc2NyaXB0aW9u\n" +
            "cwATVGhlIHF1ZXVlIHNlbGVjdG9yLgATZXhwcmVzc2lvbnMtYWxsb3dlZFoBAAhuaWxsYWJsZVoB\n" +
            "AAptaW4tbGVuZ3RoSgAAAAAAAAABAAptYXgtbGVuZ3RoSgAAAAB/////AAthY2Nlc3MtdHlwZXMA\n" +
            "CXJlYWQtb25seQAHc3RvcmFnZXMADWNvbmZpZ3VyYXRpb24ADm1lc3NhZ2VzLWFkZGVkbwAAAAYA\n" +
            "BHR5cGV0SgALZGVzY3JpcHRpb25zAEBUaGUgbnVtYmVyIG9mIG1lc3NhZ2VzIGFkZGVkIHRvIHRo\n" +
            "aXMgcXVldWUgc2luY2UgaXQgd2FzIGNyZWF0ZWQuABNleHByZXNzaW9ucy1hbGxvd2VkWgAACG5p\n" +
            "bGxhYmxlWgAAC2FjY2Vzcy10eXBlcwAGbWV0cmljAAdzdG9yYWdlcwAHcnVudGltZQAOY29uc3Vt\n" +
            "ZXItY291bnRvAAAABgAEdHlwZXRJAAtkZXNjcmlwdGlvbnMAO1RoZSBudW1iZXIgb2YgY29uc3Vt\n" +
            "ZXJzIGNvbnN1bWluZyBtZXNzYWdlcyBmcm9tIHRoaXMgcXVldWUuABNleHByZXNzaW9ucy1hbGxv\n" +
            "d2VkWgAACG5pbGxhYmxlWgAAC2FjY2Vzcy10eXBlcwAGbWV0cmljAAdzdG9yYWdlcwAHcnVudGlt\n" +
            "ZQAPc2NoZWR1bGVkLWNvdW50bwAAAAYABHR5cGV0SgALZGVzY3JpcHRpb25zAC9UaGUgbnVtYmVy\n" +
            "IG9mIHNjaGVkdWxlZCBtZXNzYWdlcyBpbiB0aGlzIHF1ZXVlLgATZXhwcmVzc2lvbnMtYWxsb3dl\n" +
            "ZFoAAAhuaWxsYWJsZVoAAAthY2Nlc3MtdHlwZXMABm1ldHJpYwAHc3RvcmFnZXMAB3J1bnRpbWUA\n" +
            "BnBhdXNlZG8AAAAGAAR0eXBldFoAC2Rlc2NyaXB0aW9ucwAcV2hldGhlciB0aGUgcXVldWUgaXMg\n" +
            "cGF1c2VkLgATZXhwcmVzc2lvbnMtYWxsb3dlZFoAAAhuaWxsYWJsZVoAAAthY2Nlc3MtdHlwZXMA\n" +
            "CXJlYWQtb25seQAHc3RvcmFnZXMAB3J1bnRpbWUACXRlbXBvcmFyeW8AAAAGAAR0eXBldFoAC2Rl\n" +
            "c2NyaXB0aW9ucwAfV2hldGhlciB0aGUgcXVldWUgaXMgdGVtcG9yYXJ5LgATZXhwcmVzc2lvbnMt\n" +
            "YWxsb3dlZFoAAAhuaWxsYWJsZVoAAAthY2Nlc3MtdHlwZXMACXJlYWQtb25seQAHc3RvcmFnZXMA\n" +
            "B3J1bnRpbWUAB2R1cmFibGVvAAAABwAEdHlwZXRaAAtkZXNjcmlwdGlvbnMAJFdoZXRoZXIgdGhl\n" +
            "IHF1ZXVlIGlzIGR1cmFibGUgb3Igbm90LgATZXhwcmVzc2lvbnMtYWxsb3dlZFoBAAhuaWxsYWJs\n" +
            "ZVoBAAdkZWZhdWx0WgEAC2FjY2Vzcy10eXBlcwAJcmVhZC1vbmx5AAdzdG9yYWdlcwANY29uZmln\n" +
            "dXJhdGlvbgANbWVzc2FnZS1jb3VudG8AAAAGAAR0eXBldEoAC2Rlc2NyaXB0aW9ucwAvVGhlIG51\n" +
            "bWJlciBvZiBtZXNzYWdlcyBjdXJyZW50bHkgaW4gdGhpcyBxdWV1ZS4AE2V4cHJlc3Npb25zLWFs\n" +
            "bG93ZWRaAAAIbmlsbGFibGVaAAALYWNjZXNzLXR5cGVzAAZtZXRyaWMAB3N0b3JhZ2VzAAdydW50\n" +
            "aW1lAA1xdWV1ZS1hZGRyZXNzbwAAAAgABHR5cGV0cwALZGVzY3JpcHRpb25zAERUaGUgcXVldWUg\n" +
            "YWRkcmVzcyBkZWZpbmVzIHdoYXQgYWRkcmVzcyBpcyB1c2VkIGZvciByb3V0aW5nIG1lc3NhZ2Vz\n" +
            "LgATZXhwcmVzc2lvbnMtYWxsb3dlZFoAAAhuaWxsYWJsZVoAAAptaW4tbGVuZ3RoSgAAAAAAAAAB\n" +
            "AAptYXgtbGVuZ3RoSgAAAAB/////AAthY2Nlc3MtdHlwZXMACXJlYWQtb25seQAHc3RvcmFnZXMA\n" +
            "B3J1bnRpbWUADmV4cGlyeS1hZGRyZXNzbwAAAAgABHR5cGV0cwALZGVzY3JpcHRpb25zAChUaGUg\n" +
            "YWRkcmVzcyB0byBzZW5kIGV4cGlyZWQgbWVzc2FnZXMgdG8uABNleHByZXNzaW9ucy1hbGxvd2Vk\n" +
            "WgAACG5pbGxhYmxlWgEACm1pbi1sZW5ndGhKAAAAAAAAAAEACm1heC1sZW5ndGhKAAAAAH////8A\n" +
            "C2FjY2Vzcy10eXBlcwAJcmVhZC1vbmx5AAdzdG9yYWdlcwAHcnVudGltZQAHZW50cmllc28AAAAH\n" +
            "AAR0eXBldGwAC2Rlc2NyaXB0aW9ucwAqVGhlIGpuZGkgbmFtZXMgdGhlIHF1ZXVlIHdpbGwgYmUg\n" +
            "Ym91bmQgdG8uABNleHByZXNzaW9ucy1hbGxvd2VkWgEACG5pbGxhYmxlWgAACnZhbHVlLXR5cGV0\n" +
            "cwALYWNjZXNzLXR5cGVzAAlyZWFkLW9ubHkAB3N0b3JhZ2VzAA1jb25maWd1cmF0aW9uABNkZWFk\n" +
            "LWxldHRlci1hZGRyZXNzbwAAAAgABHR5cGV0cwALZGVzY3JpcHRpb25zACVUaGUgYWRkcmVzcyB0\n" +
            "byBzZW5kIGRlYWQgbWVzc2FnZXMgdG8uABNleHByZXNzaW9ucy1hbGxvd2VkWgAACG5pbGxhYmxl\n" +
            "WgEACm1pbi1sZW5ndGhKAAAAAAAAAAEACm1heC1sZW5ndGhKAAAAAH////8AC2FjY2Vzcy10eXBl\n" +
            "cwAJcmVhZC1vbmx5AAdzdG9yYWdlcwAHcnVudGltZQAQZGVsaXZlcmluZy1jb3VudG8AAAAGAAR0\n" +
            "eXBldEkAC2Rlc2NyaXB0aW9ucwBQVGhlIG51bWJlciBvZiBtZXNzYWdlcyB0aGF0IHRoaXMgcXVl\n" +
            "dWUgaXMgY3VycmVudGx5IGRlbGl2ZXJpbmcgdG8gaXRzIGNvbnN1bWVycy4AE2V4cHJlc3Npb25z\n" +
            "LWFsbG93ZWRaAAAIbmlsbGFibGVaAAALYWNjZXNzLXR5cGVzAAZtZXRyaWMAB3N0b3JhZ2VzAAdy\n" +
            "dW50aW1lAApvcGVyYXRpb25zdQAIY2hpbGRyZW5vAAAAAA==";
}
