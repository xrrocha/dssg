const world = "world";

export function hello(name: string = world): string {
    return `Hello ${name}!`
}

console.log(`Empty invocation of hello function ${hello()}`)
console.log(`Parameterized invocation of the hello function ${hello("Regina")}`)
