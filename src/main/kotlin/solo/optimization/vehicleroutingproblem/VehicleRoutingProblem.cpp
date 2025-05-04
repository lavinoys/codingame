#include <iostream>
#include <vector>
#include <algorithm>
#include <cmath>
#include <unordered_map>
#include <string>
#include <sstream>
#include <chrono>
#include <random>
#include <climits>

using namespace std;

// 고객 클래스 정의
struct Customer {
    int index;
    int x;
    int y;
    int demand;

    Customer(int idx, int x_coord, int y_coord, int d) :
        index(idx), x(x_coord), y(y_coord), demand(d) {}
};

// 전역 변수들
vector<vector<int>> distanceCache;
unordered_map<int, int> customerIndexToArrayIndex;

// 두 고객 간의 거리 계산 (캐시 활용)
int getDistance(const Customer& a, const Customer& b) {
    int aIndex = customerIndexToArrayIndex[a.index];
    int bIndex = customerIndexToArrayIndex[b.index];

    if (distanceCache[aIndex][bIndex] == 0 && aIndex != bIndex) {
        int dx = a.x - b.x;
        int dy = a.y - b.y;
        int distance = sqrt(dx*dx + dy*dy);
        distanceCache[aIndex][bIndex] = distance;
        distanceCache[bIndex][aIndex] = distance; // 대칭성 활용
    }

    return distanceCache[aIndex][bIndex];
}

// 거리 캐시 초기화 함수
void initDistanceCache(const vector<Customer>& customers) {
    int customerCount = customers.size();
    distanceCache.resize(customerCount, vector<int>(customerCount, 0));

    // Customer.index와 배열 인덱스 간의 매핑 생성
    for (int i = 0; i < customerCount; i++) {
        customerIndexToArrayIndex[customers[i].index] = i;
    }
}

// 경로의 총 거리 계산
int calculateRouteDistance(const Customer& depot, const vector<Customer>& route) {
    if (route.empty()) return 0;

    int distance = getDistance(depot, route[0]); // depot에서 첫 고객까지

    // 고객 간 이동 거리
    for (int i = 0; i < route.size() - 1; i++) {
        distance += getDistance(route[i], route[i + 1]);
    }

    // 마지막 고객에서 depot로 돌아가는 거리
    distance += getDistance(route.back(), depot);

    return distance;
}

// 경로의 총 수요 계산
int calculateRouteDemand(const vector<Customer>& route) {
    int totalDemand = 0;
    for (const auto& customer : route) {
        totalDemand += customer.demand;
    }
    return totalDemand;
}

// 탐욕 알고리즘으로 초기 솔루션 생성
vector<vector<Customer>> greedySolution(const Customer& depot, vector<Customer> customers, int capacity) {
    vector<vector<Customer>> routes;

    // 고객들이 모두 처리될 때까지 반복
    while (!customers.empty()) {
        vector<Customer> route;
        int remainingCapacity = capacity;

        // 현재 포인트 (시작은 depot)
        Customer current = depot;

        // 더 이상 고객을 추가할 수 없을 때까지 반복
        while (!customers.empty()) {
            // 가장 가까운 고객 찾기
            int nearestIdx = -1;
            int minDistance = INT_MAX;

            for (int i = 0; i < customers.size(); i++) {
                if (customers[i].demand <= remainingCapacity) {
                    int distance = getDistance(current, customers[i]);
                    if (distance < minDistance) {
                        minDistance = distance;
                        nearestIdx = i;
                    }
                }
            }

            // 더 이상 추가할 수 있는 고객이 없으면 종료
            if (nearestIdx == -1) break;

            // 가장 가까운 고객 추가
            current = customers[nearestIdx];
            route.push_back(current);
            remainingCapacity -= current.demand;

            // 목록에서 제거
            customers.erase(customers.begin() + nearestIdx);
        }

        // 경로가 비어있지 않으면 추가
        if (!route.empty()) {
            routes.push_back(route);
        }
    }

    return routes;
}

// 시뮬레이티드 어닐링으로 해결책 개선
vector<vector<Customer>> simulatedAnnealing(
    const Customer& depot, const vector<vector<Customer>>& initialRoutes, int capacity) {

    vector<vector<Customer>> currentSolution = initialRoutes;
    vector<vector<Customer>> bestSolution = initialRoutes;

    // 초기 에너지 계산 (총 거리)
    int currentEnergy = 0;
    for (const auto& route : currentSolution) {
        currentEnergy += calculateRouteDistance(depot, route);
    }
    int bestEnergy = currentEnergy;

    // 시뮬레이티드 어닐링 파라미터
    double startTemp = 100.0;
    double temperature = startTemp;
    double coolingRate = 0.995;

    // 랜덤 엔진 설정
    unsigned seed = chrono::system_clock::now().time_since_epoch().count();
    default_random_engine generator(seed);
    uniform_real_distribution<double> distribution(0.0, 1.0);

    // 시간 제한 설정 (4.5초)
    auto startTime = chrono::steady_clock::now();
    auto endTime = startTime + chrono::milliseconds(4500);

    int iterations = 0;
    const int checkInterval = 500;

    // 메인 시뮬레이티드 어닐링 루프
    while (chrono::steady_clock::now() < endTime && temperature > 0.1) {
        iterations++;

        // 이웃 솔루션 생성
        vector<vector<Customer>> newSolution = currentSolution;

        // 랜덤 이동 선택 (몇 가지 이동 전략 중 하나)
        int moveType = distribution(generator) * 3;

        if (moveType == 0 && newSolution.size() >= 2) {
            // 두 경로 간 고객 교환
            int routeIdx1 = distribution(generator) * newSolution.size();
            int routeIdx2 = distribution(generator) * newSolution.size();

            if (routeIdx1 != routeIdx2 && !newSolution[routeIdx1].empty() && !newSolution[routeIdx2].empty()) {
                int custIdx1 = distribution(generator) * newSolution[routeIdx1].size();
                int custIdx2 = distribution(generator) * newSolution[routeIdx2].size();

                // 교환 후 용량 확인
                Customer temp = newSolution[routeIdx1][custIdx1];
                int demand1 = calculateRouteDemand(newSolution[routeIdx1]) - temp.demand + newSolution[routeIdx2][custIdx2].demand;
                int demand2 = calculateRouteDemand(newSolution[routeIdx2]) - newSolution[routeIdx2][custIdx2].demand + temp.demand;

                if (demand1 <= capacity && demand2 <= capacity) {
                    swap(newSolution[routeIdx1][custIdx1], newSolution[routeIdx2][custIdx2]);
                }
            }
        }
        else if (moveType == 1) {
            // 경로 내 고객 순서 변경 (2-opt)
            if (!newSolution.empty()) {
                int routeIdx = distribution(generator) * newSolution.size();
                if (newSolution[routeIdx].size() >= 3) {
                    int i = 1 + distribution(generator) * (newSolution[routeIdx].size() - 2);
                    int j = i + 1 + distribution(generator) * (newSolution[routeIdx].size() - i - 1);
                    reverse(newSolution[routeIdx].begin() + i, newSolution[routeIdx].begin() + j + 1);
                }
            }
        }
        else {
            // 고객을 다른 경로로 이동
            if (newSolution.size() >= 2) {
                int fromIdx = distribution(generator) * newSolution.size();
                int toIdx = distribution(generator) * newSolution.size();

                if (fromIdx != toIdx && !newSolution[fromIdx].empty()) {
                    int custIdx = distribution(generator) * newSolution[fromIdx].size();
                    Customer cust = newSolution[fromIdx][custIdx];

                    // 이동 후 용량 확인
                    int newDemand = calculateRouteDemand(newSolution[toIdx]) + cust.demand;
                    if (newDemand <= capacity) {
                        newSolution[fromIdx].erase(newSolution[fromIdx].begin() + custIdx);
                        newSolution[toIdx].push_back(cust);
                    }
                }
            }
        }

        // 빈 경로 제거
        newSolution.erase(
            remove_if(newSolution.begin(), newSolution.end(),
                [](const vector<Customer>& route) { return route.empty(); }),
            newSolution.end()
        );

        // 새 에너지 계산
        int newEnergy = 0;
        for (const auto& route : newSolution) {
            newEnergy += calculateRouteDistance(depot, route);
        }

        // 에너지 변화
        int delta = newEnergy - currentEnergy;

        // 더 나은 해결책이거나 확률에 따라 나쁜 해결책 수용
        if (delta < 0 || distribution(generator) < exp(-delta / temperature)) {
            currentSolution = newSolution;
            currentEnergy = newEnergy;

            if (currentEnergy < bestEnergy) {
                bestSolution = currentSolution;
                bestEnergy = currentEnergy;
            }
        }

        // 주기적으로만 시간 체크 및 온도 감소
        if (iterations % checkInterval == 0) {
            if (chrono::steady_clock::now() >= endTime) break;
            temperature *= coolingRate;
        }
    }

    return bestSolution;
}

// 해결책을 출력 형식으로 변환
string formatSolution(const vector<vector<Customer>>& routes) {
    stringstream ss;

    for (int i = 0; i < routes.size(); i++) {
        for (int j = 0; j < routes[i].size(); j++) {
            ss << routes[i][j].index;
            if (j < routes[i].size() - 1) {
                ss << " ";
            }
        }

        if (i < routes.size() - 1) {
            ss << ";";
        }
    }

    return ss.str();
}

int main() {
    int n; // 고객 수
    cin >> n;
    int c; // 차량 용량
    cin >> c;

    vector<Customer> customers;

    for (int i = 0; i <= n; i++) {
        int index, x, y, demand;
        cin >> index >> x >> y >> demand;
        customers.push_back(Customer(index, x, y, demand));
    }

    // 창고(depot)는 항상 인덱스 0
    Customer depot = customers[0];

    // 실제 고객들만 분리 (창고 제외)
    vector<Customer> actualCustomers;
    for (int i = 1; i < customers.size(); i++) {
        actualCustomers.push_back(customers[i]);
    }

    // 거리 캐시 초기화
    initDistanceCache(customers);

    // 1. 탐욕 알고리즘으로 초기 해결책 생성
    vector<vector<Customer>> initialSolution = greedySolution(depot, actualCustomers, c);

    // 빠른 첫번째 응답을 위해 초기 해결책 즉시 출력
    cout << formatSolution(initialSolution) << endl;

    // 2. 시뮬레이티드 어닐링으로 해결책 개선
    vector<vector<Customer>> finalSolution = simulatedAnnealing(depot, initialSolution, c);

    // 최종 결과 출력
    cout << formatSolution(finalSolution) << endl;

    return 0;
}
